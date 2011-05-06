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
import org.esa.cci.sst.reader.MmdIOHandler;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.ToolException;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Tool responsible for re-ingesting mmd files.
 *
 * @author Thomas Storm
 */
public class MmdIngestionTool extends BasicTool {

    public static void main(String[] args) {
        final MmdIngestionTool tool = new MmdIngestionTool();
        final boolean performWork = tool.setCommandLineArgs(args);
        if (!performWork) {
            return;
        }
        tool.initialize();

        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        try {
            persistenceManager.transaction();
            tool.ingest();
            persistenceManager.commit();
        } catch (Exception e) {
            try {
                persistenceManager.rollback();
            } catch (Exception _) {
                // ignore
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }


    private static final String MMS_REINGESTION_SENSOR_PROPERTY = "mms.reingestion.sensor";
    private static final String MMS_REINGESTION_PATTERN_PROPERTY = "mms.reingestion.pattern";
    private static final String DATAFILE_ALREADY_INGESTED = "SELECT COUNT(*) " +
                                                            "FROM mm_datafile " +
                                                            "WHERE path = '%s'";

    private MmdIOHandler ioHandler;
    private final Ingester ingester = new Ingester(this);
    private DataFile dataFile;

    private MmdIngestionTool() {
        super("mmsingest.sh", "0.1");
    }

    void ingest() {
        final String sensorName = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        final String patternProperty = getConfiguration().getProperty(MMS_REINGESTION_PATTERN_PROPERTY, "0");
        final long pattern = Long.parseLong(patternProperty, 16);
        final boolean located = "yes".equals(getConfiguration().getProperty("mms.reingestion.located", "no"));
        Sensor sensor = getSensor(sensorName);
        final boolean persistVariables = (sensor == null);
        if (persistVariables) {
            sensor = ingester.createSensor(sensorName, located ? "RelatedObservation" : "Observation", pattern);
        }
        DataFile dataFile = createDataFile(sensor);
        initIOHandler(dataFile);
        if (persistVariables) {
            persistColumns(sensorName);
        }
        ingestObservations();
    }

    private void ingestObservations() {
        final MmdObservationIngester observationIngester = new MmdObservationIngester(this, ingester, ioHandler);
        observationIngester.ingestObservations();
    }

    private void persistColumns(String sensorName) {
        try {
            ingester.persistColumns(sensorName, ioHandler);
        } catch (IOException e) {
            throw new ToolException(
                    MessageFormat.format("Unable to persist columns for sensor ''{0}''.", sensorName),
                    e,
                    ToolException.TOOL_ERROR);
        }
    }

    private DataFile createDataFile(Sensor sensor) {
        final File mmdFile = getMmdFile();
        dataFile = new DataFile(mmdFile, sensor);
        storeDataFile();
        return dataFile;
    }

    private void storeDataFile() {
        final String queryString = String.format(DATAFILE_ALREADY_INGESTED, dataFile.getPath());
        storeOnce(dataFile, queryString);
    }

    private void storeOnce(final Object data, final String queryString) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        final Query query = persistenceManager.createNativeQuery(queryString, Integer.class);
        int result = (Integer) query.getSingleResult();
        if (result == 0) {
            persistenceManager.persist(data);
        } else {
            throw new IllegalStateException("Trying to ingest duplicate datafile.");
        }
    }

    private File getMmdFile() {
        final Properties configuration = getConfiguration();
        final String filename = configuration.getProperty("mms.reingestion.filename");
        return new File(filename);
    }

    private void initIOHandler(final DataFile dataFile) {
        ioHandler = new MmdIOHandler(getConfiguration());
        try {
            ioHandler.init(dataFile);
        } catch (IOException e) {
            throw new ToolException("Error initializing IOHandler for mmd file.", e, ToolException.TOOL_ERROR);
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
