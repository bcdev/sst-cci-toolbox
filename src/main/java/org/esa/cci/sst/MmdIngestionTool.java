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

package org.esa.cci.sst;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.util.DataUtil;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;

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
        tool.ingest();
    }

    private IngestionTool delegate;

    MmdIngestionTool() throws ToolException {
        super("mmdingest.sh", "0.1");
    }

    public void init(final String[] args) throws ToolException {
        delegate = new IngestionTool();
        delegate.setCommandLineArgs(args);
        delegate.initialize();
    }

    private void ingest() throws ToolException {
        final IOHandler ioHandler = new MmdReader(delegate.getPersistenceManager());
        final File mmdFile = getMmdFile();
        final DataFile dataFile = getDataFile(mmdFile);
        initIOHandler(ioHandler, dataFile);
        final int numRecords = ioHandler.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            getLogger().info("ingestion of record '" + (i + 1) + "/" + numRecords + "'");
            persistObservation(ioHandler, i);
        }
    }

    private DataFile getDataFile(final File file) {

        // todo - ts 5Apr2011 - data file has to be ingested, too, if that has not yet happened

        final String sensorType = "ARC";   // todo - ts 4Apr2011 - ok?
        final DataSchema dataSchema = DataUtil.createDataSchema(Constants.DATA_SCHEMA_NAME_MMD, sensorType);
        return DataUtil.createDataFile(file, dataSchema);
    }

    private File getMmdFile() {
        final String filename = getConfiguration().getProperty("mms.test.arc3.output.filename", "mmd.nc");
        return new File(filename);
    }

    private void persistObservation(final IOHandler ioHandler, int recordNo) throws ToolException {
        final PersistenceManager persistenceManager = delegate.getPersistenceManager();
        persistenceManager.transaction();
        try {
            delegate.persistObservation(ioHandler, recordNo);
        } catch (Exception e) {
            getErrorHandler().handleError(e, MessageFormat.format("Error persisting observation ''{0}''.", recordNo),
                                          ToolException.TOOL_ERROR);
        } finally {
            persistenceManager.commit();
        }
    }

    private void initIOHandler(final IOHandler ioHandler, final DataFile dataFile) throws ToolException {
        try {
            ioHandler.init(dataFile);
        } catch (IOException e) {
            getErrorHandler().handleError(e, "Error initializing IOHandler for mmd file", ToolException.TOOL_ERROR);
        }
    }

    void closeReader(final Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
