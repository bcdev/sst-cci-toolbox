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
 * MmsTool responsible for ingesting mmd files which have been processed by ARC3. Uses {@link IngestionTool} as delegate.
 *
 * @author Thomas Storm
 */
public class MmdIngestionTool extends MmsTool {

    public static void main(String[] args) throws ToolException {
        final MmdIngestionTool tool = new MmdIngestionTool();
        final boolean performWork = tool.setCommandLineArgs(args);
        if (!performWork) {
            return;
        }
        tool.init(args);
        ingestDataInfo(tool);
        ingestVariableDescriptors(tool);
        ingestObservations(tool);
        ingestCoincidences(tool);
    }

    private static void ingestDataInfo(final MmdIngestionTool tool) {
        final MmdDataInfoIngester mmdDataInfoIngester = new MmdDataInfoIngester(tool);
        mmdDataInfoIngester.ingestDataFile();
        mmdDataInfoIngester.ingestDataSchema();
    }

    private static void ingestVariableDescriptors(final MmdIngestionTool tool) throws ToolException {
        tool.ingestVariableDescriptors();
    }

    private static void ingestObservations(final MmdIngestionTool tool) throws ToolException {
        final MmdObservationIngester observationIngester = new MmdObservationIngester(tool);
        observationIngester.ingestObservations();
    }

    private static void ingestCoincidences(final MmdIngestionTool tool) throws ToolException {
        final MmdCoincidenceIngester coincidenceIngester = new MmdCoincidenceIngester(tool);
        coincidenceIngester.ingestCoincidences();
    }

    private IngestionTool delegate;
    private IOHandler ioHandler;
    private File mmdFile;

    private final Map<File, DataFile> dataFileMap = new HashMap<File, DataFile>();

    MmdIngestionTool() throws ToolException {
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

    private DataFile getDataFile(final File file) {
        if (dataFileMap.get(file) == null) {
            final DataFile dataFile = DataUtil.createDataFile(file, MmdDataInfoIngester.DATA_SCHEMA);
            dataFileMap.put(file, dataFile);
            return dataFile;
        }
        return dataFileMap.get(file);
    }

    private void ingestVariableDescriptors() throws ToolException {
        try {
            delegate.persistVariableDescriptors("mmd", "ARC3", ioHandler);
        } catch (IOException e) {
            throw new ToolException("Unable to persist variable descriptors for sensor 'ARC'.", e,
                                    ToolException.TOOL_ERROR);
        }
    }

    private void initIOHandler(final DataFile dataFile) throws ToolException {
        ioHandler = new MmdReader(delegate.getPersistenceManager());
        try {
            ioHandler.init(dataFile);
        } catch (IOException e) {
            getErrorHandler().handleError(e, "Error initializing IOHandler for mmd file.", ToolException.TOOL_ERROR);
        }
    }
}
