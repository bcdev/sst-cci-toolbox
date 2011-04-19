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
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.DataUtil;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Responsible for re-ingesting mmd files.
 *
 * @author Thomas Storm
 */
public class MmdIngester extends MmsTool {

    private static final String MMS_REINGESTION_SCHEMANAME_PROPERTY = "mms.reingestion.schemaname";
    private static final String MMS_REINGESTION_SENSORTYPE_PROPERTY = "mms.reingestion.sensortype";
    private static final String MMS_REINGESTION_SENSOR_PROPERTY = "mms.reingestion.sensor";
    private IngestionTool delegate;
    private IOHandler ioHandler;
    private File mmdFile;
    private DataSchema dataSchema;

    private final Map<File, DataFile> dataFileMap = new HashMap<File, DataFile>();

    MmdIngester() throws ToolException {
        super("mmdingest.sh", "0.1");
    }

    @Override
    public PersistenceManager getPersistenceManager() {
        return delegate.getPersistenceManager();
    }

    void init(final String[] args) throws ToolException {
        delegate = new IngestionTool();
        delegate.setCommandLineArgs(args);
        delegate.initialize();
        initIOHandler(getDataFile());
    }

    DataFile getDataFile() {
        final File mmdFile = getMmdFile();
        return getDataFile(mmdFile);
    }

    IngestionTool getDelegate() {
        return delegate;
    }

    IOHandler getIoHandler() {
        return ioHandler;
    }

    void ingestVariableDescriptors() throws ToolException {
        final String schemaName = getProperty(MMS_REINGESTION_SCHEMANAME_PROPERTY);
        final String sensor = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        final PersistenceManager persistenceManager = getPersistenceManager();
        persistenceManager.transaction();
        try {
            delegate.persistVariableDescriptors(schemaName, sensor, ioHandler);
        } catch (IOException e) {
            throw new ToolException(
                    MessageFormat.format("Unable to persist variable descriptors for sensor ''{0}''.", sensor), e,
                                    ToolException.TOOL_ERROR);
        } finally {
            persistenceManager.commit();
        }
    }

    DataSchema getDataSchema() {
        if (dataSchema == null) {
            final String sensorType = getProperty(MMS_REINGESTION_SENSORTYPE_PROPERTY);
            final String schemaName = getProperty(MMS_REINGESTION_SCHEMANAME_PROPERTY);
            dataSchema = DataUtil.createDataSchema(schemaName, sensorType);
        }
        return dataSchema;
    }

    private File getMmdFile() {
        if (mmdFile == null) {
            final Properties configuration = getConfiguration();
            final String filename = configuration.getProperty("mms.reingestion.filename");
            mmdFile = new File(filename);
        }
        return mmdFile;
    }

    private DataFile getDataFile(final File file) {
        if (dataFileMap.get(file) == null) {
            final DataFile dataFile = DataUtil.createDataFile(file, getDataSchema());
            dataFileMap.put(file, dataFile);
            return dataFile;
        }
        return dataFileMap.get(file);
    }

    private void initIOHandler(final DataFile dataFile) throws ToolException {
        final String sensor = getProperty(MMS_REINGESTION_SENSOR_PROPERTY);
        final String schemaName = getProperty(MMS_REINGESTION_SCHEMANAME_PROPERTY);
        ioHandler = new MmdReader(delegate.getPersistenceManager(), sensor, schemaName);
        try {
            ioHandler.init(dataFile);
        } catch (IOException e) {
            getErrorHandler().handleError(e, "Error initializing IOHandler for mmd file.", ToolException.TOOL_ERROR);
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
