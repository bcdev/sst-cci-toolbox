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
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.DataUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Responsible for re-ingesting mmd files.
 *
 * @author Thomas Storm
 */
public class MmdIngester extends MmsTool {

    private IngestionTool delegate;
    private IOHandler ioHandler;
    private File mmdFile;

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

    File getMmdFile() {
        final Properties configuration = getConfiguration();
        final String filename = configuration.getProperty("mms.test.arc3.output.filename", "mmd.nc");
        if (mmdFile == null) {
            mmdFile = new File(filename);
        }
        return mmdFile;
    }

    IngestionTool getDelegate() {
        return delegate;
    }

    IOHandler getIoHandler() {
        return ioHandler;
    }

    DataFile getDataFile(final File file) {
        if (dataFileMap.get(file) == null) {
            final DataFile dataFile = DataUtil.createDataFile(file, MmdDataInfoIngester.DATA_SCHEMA);
            dataFileMap.put(file, dataFile);
            return dataFile;
        }
        return dataFileMap.get(file);
    }

    void ingestVariableDescriptors() throws ToolException {
        try {
            delegate.persistVariableDescriptors("mmd", "ARC3", ioHandler);
        } catch (IOException e) {
            throw new ToolException("Unable to persist variable descriptors for sensor 'ARC'.", e,
                                    ToolException.TOOL_ERROR);
        }
    }

    void initIOHandler(final DataFile dataFile) throws ToolException {
        ioHandler = new MmdReader(delegate.getPersistenceManager());
        try {
            ioHandler.init(dataFile);
        } catch (IOException e) {
            getErrorHandler().handleError(e, "Error initializing IOHandler for mmd file.", ToolException.TOOL_ERROR);
        }
    }

}
