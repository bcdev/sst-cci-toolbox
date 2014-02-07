/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.GunzipDecorator;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;

import javax.persistence.EntityExistsException;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Tool responsible for re-ingesting mmd files.
 *
 * @author Thomas Storm
 */
public class MmdIngestionTool extends BasicTool {

    public static void main(String[] args) {
        final MmdIngestionTool tool = new MmdIngestionTool();
        final boolean ok = tool.setCommandLineArgs(args);
        if (ok) {
            tool.initialize();
            tool.ingest();
            tool.getPersistenceManager().close();
        } else {
            tool.printHelp();
        }
    }


    private static final String MMS_REINGESTION_SENSOR_PROPERTY = "mms.reingestion.sensor";
    private static final String MMS_REINGESTION_PATTERN_PROPERTY = "mms.reingestion.pattern";
    private static final String DATAFILE_ALREADY_INGESTED = "SELECT COUNT(*) " +
            "FROM mm_datafile " +
            "WHERE path = '%s'";

    private MmdReader reader;
    private Ingester ingester;
    private DataFile dataFile;

    private MmdIngestionTool() {
        super("reingestion-tool.sh", "0.1");
    }

    void ingest() {
        ingester = new Ingester(this);
        final String sensorName = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        final String patternProperty = getConfiguration().getProperty(MMS_REINGESTION_PATTERN_PROPERTY, "0");
        final long pattern = Long.parseLong(patternProperty, 16);
        final String locatedString = getConfiguration().getProperty("mms.reingestion.located", "false");
        final boolean located = Boolean.parseBoolean(locatedString) /* for backward compatibility */ || "yes".equals(locatedString);
        final boolean withOverwrite = Boolean.parseBoolean(getConfiguration().getProperty("mms.reingestion.overwrite"));
        String path = getConfiguration().getProperty(Constants.PROPERTY_MMS_REINGESTION_FILENAME);
        final String archiveRootPath = getConfiguration().getProperty(Constants.PROPERTY_ARCHIVE_ROOTDIR);
        final File archiveRoot = new File(archiveRootPath);
        if (path.startsWith(archiveRootPath + File.separator)) {
            path = path.substring(archiveRootPath.length() + 1);
        }
        try {
            getPersistenceManager().transaction();
            Sensor sensor = getSensor(sensorName);
            final boolean persistSensor = (sensor == null);
            if (persistSensor) {
                try {
                    sensor = ingester.createSensor(sensorName, located ? "RelatedObservation" : "Observation", pattern);
                    // make sensor entry visible to concurrent processes to avoid duplicate creation
                } catch (EntityExistsException e) {
                    sensor = getSensor(sensorName);
                }
            }
            getPersistenceManager().commit();

            if (withOverwrite) {
                getPersistenceManager().transaction();
                dataFile = getDataFile(path);
                if (dataFile != null) {
                    dropObservationsAndCoincidencesOf(dataFile);
                }
                getPersistenceManager().commit();
            }
            boolean isNewDatafile = dataFile == null;
            if (isNewDatafile) {
                createDataFile(sensor, path);
            }

            initReader(dataFile, archiveRoot);

            getPersistenceManager().transaction();
            persistColumns(sensorName);
            getPersistenceManager().commit();

            getPersistenceManager().transaction();
            if (isNewDatafile) {
                storeDataFile();
            }
            ingestObservations(pattern);
            getPersistenceManager().commit();
        } catch (Exception e) {
            try {
                getPersistenceManager().rollback();
            } catch (Exception _) {
                // ignore
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private void dropObservationsAndCoincidencesOf(DataFile dataFile) {

//            Query query = getPersistenceManager().createQuery("delete from Coincidence where exists " +
//                                                              "( select f from DataFile f, Observation o " +
//                                                              "  where f.path = ?1 and o.dataFile = f and c.observation = o )");
        Query query = getPersistenceManager().createNativeQuery("delete from mm_coincidence c " +
                "where exists ( select f.id from mm_datafile f, mm_observation o " +
                "where f.path = ?1 and o.datafile_id = f.id and c.observation_id = o.id )");
        query.setParameter(1, dataFile.getPath());
        long time = System.currentTimeMillis();
        query.executeUpdate();
        getLogger().info(MessageFormat.format("{0} coincidences dropped in {1} ms.", dataFile.getPath(),
                System.currentTimeMillis() - time));

        query = getPersistenceManager().createNativeQuery("delete from mm_observation o " +
                "where exists ( select f.id from mm_datafile f " +
                "where f.path = ?1 and o.datafile_id = f.id )");
        query.setParameter(1, dataFile.getPath());
        time = System.currentTimeMillis();
        query.executeUpdate();
        getLogger().info(MessageFormat.format("{0} observations dropped in {1} ms.", dataFile.getPath(),
                System.currentTimeMillis() - time));
    }

    private void ingestObservations(long pattern) {
        final MmdObservationIngester observationIngester = new MmdObservationIngester(this, ingester, reader, pattern);
        observationIngester.ingestObservations();
    }

    private void persistColumns(String sensorName) {
        try {
            ingester.persistColumns(sensorName, reader);
        } catch (IOException e) {
            throw new ToolException(
                    MessageFormat.format("Unable to persist columns for sensor ''{0}''.", sensorName),
                    e,
                    ToolException.TOOL_ERROR);
        }
    }

    private DataFile createDataFile(Sensor sensor, String path) {
        final File mmdFile = new File(path);
        dataFile = new DataFile(mmdFile, sensor);
        return dataFile;
    }

    private void storeDataFile() {
        final String queryString = String.format(DATAFILE_ALREADY_INGESTED, dataFile.getPath());
        storeOnce(dataFile, queryString);
    }

    private void storeOnce(final DataFile datafile, final String queryString) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        final Query query = persistenceManager.createNativeQuery(queryString, Integer.class);
        int result = (Integer) query.getSingleResult();
        if (result == 0) {
            persistenceManager.persist(datafile);
        } else {
            throw new IllegalStateException("Trying to ingest duplicate datafile.");
        }
    }

    private void initReader(final DataFile dataFile, File archiveRoot) {
        reader = new MmdReader(dataFile.getSensor().getName());
        GunzipDecorator decorator = new GunzipDecorator(reader);
        reader.setConfiguration(getConfiguration());
        try {
            decorator.init(dataFile, archiveRoot);
        } catch (IOException e) {
            final File filePath = new File(archiveRoot, dataFile.getPath());
            throw new ToolException("Error initializing Reader for MMD file '" + filePath + "'.", e, ToolException.TOOL_ERROR);
        }
    }

    private String getProperty(String key) {
        final String property = getConfiguration().getProperty(key);
        if (property == null) {
            throw new IllegalStateException(
                    MessageFormat.format("Property ''{0}'' not specified in config file.", key));
        }
        return property;
    }

}
