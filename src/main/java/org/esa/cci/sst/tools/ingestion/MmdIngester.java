/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.MmdIOHandler;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.DataUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Responsible for re-ingesting mmd files.
 *
 * @author Thomas Storm
 */
public class MmdIngester extends BasicTool {

    private static final String MMS_REINGESTION_SENSOR_PROPERTY = "mms.reingestion.sensor";
    private static final String MMS_REINGESTION_PATTERN_PROPERTY = "mms.reingestion.pattern";
    private static final String DATAFILE_ALREADY_INGESTED = "SELECT COUNT(*) " +
                                                            "FROM mm_datafile " +
                                                            "WHERE path = '%s'";
    private IngestionTool delegate;
    private IOHandler ioHandler;
    private DataFile dataFile;

    MmdIngester() {
        super("mmdingest.sh", "0.1");
    }

    void init(String[] args) {
        super.initialize();
        delegate = new IngestionTool();
        delegate.setCommandLineArgs(args);
        delegate.initialize();
    }

    void ingest() {
        final String sensorName = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        final String patternProperty = getConfiguration().getProperty(MMS_REINGESTION_PATTERN_PROPERTY, "0");
        final long pattern = Long.parseLong(patternProperty, 16);
        final boolean located = "yes".equals(getConfiguration().getProperty("mms.reingestion.located", "no"));
        Sensor sensor = delegate.getSensor(sensorName);
        final boolean persistVariables = (sensor == null);
        if (persistVariables) {
            sensor = delegate.createSensor(sensorName, located ? "RelatedObservation" : "Observation", pattern);
        }
        DataFile dataFile = createDataFile(sensor);
        initIOHandler(dataFile);
        if (persistVariables) {
            persistVariables(sensorName);
        }
        ingestObservations();
    }

    IngestionTool getDelegate() {
        return delegate;
    }

    IOHandler getIoHandler() {
        return ioHandler;
    }

    private void ingestObservations() {
        final MmdObservationIngester observationIngester = new MmdObservationIngester(this);
        observationIngester.ingestObservations();
    }

    private void persistVariables(String sensorName) {
        try {
            delegate.persistColumns(sensorName, ioHandler);
        } catch (IOException e) {
            throw new ToolException(
                    MessageFormat.format("Unable to persist columns for sensor ''{0}''.", sensorName),
                    e,
                    ToolException.TOOL_ERROR);
        }
    }

    private DataFile createDataFile(Sensor sensor) {
        final File mmdFile = getMmdFile();
        dataFile = DataUtil.createDataFile(mmdFile, sensor);
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
        final String sensor = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        ioHandler = new MmdIOHandler(delegate.getConfiguration());
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
