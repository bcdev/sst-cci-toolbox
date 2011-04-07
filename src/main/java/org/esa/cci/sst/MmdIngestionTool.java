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

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

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
        tool.ingestDataFile();
        tool.ingestDataSchema();
        tool.ingestVariableDescriptors();
        tool.ingestObservations();
        tool.ingestCoincidences();
    }

    private static final String DATAFILE_ALREADY_INGESTED_QUERY = "SELECT COUNT (id) " +
                                                                  "FROM mm_datafile " +
                                                                  "WHERE path = %s";

    private static final String DATASCHEMA_ALREADY_INGESTED_QUERY = "SELECT COUNT (id) " +
                                                                    "FROM mm_dataschema " +
                                                                    "WHERE name = %s";


    private IngestionTool delegate;
    private File mmdFile;
    private IOHandler ioHandler;

    private final DataSchema dataSchema = DataUtil.createDataSchema(Constants.DATA_SCHEMA_NAME_MMD, "ARC");
    private final Map<File, DataFile> dataFileMap = new HashMap<File, DataFile>();

    MmdIngestionTool() throws ToolException {
        super("mmdingest.sh", "0.1");
    }

    public void init(final String[] args) throws ToolException {
        delegate = new IngestionTool();
        delegate.setCommandLineArgs(args);
        delegate.initialize();
        ioHandler = new MmdReader(delegate.getPersistenceManager());
    }

    private void ingestDataFile() {
        final DataFile dataFile = getDataFile();
        ingestOnce(dataFile, DATAFILE_ALREADY_INGESTED_QUERY);
    }

    private void ingestDataSchema() {
        ingestOnce(dataSchema, DATASCHEMA_ALREADY_INGESTED_QUERY);
    }

    private void ingestVariableDescriptors() throws ToolException {
        try {
            delegate.persistVariableDescriptors("mmd", "ARC3", ioHandler);
        } catch (IOException e) {
            throw new ToolException("Unable to persist variable descriptors for sensor 'ARC'.", e,
                                    ToolException.TOOL_ERROR);
        }
    }

    private void ingestObservations() throws ToolException {
        final DataFile dataFile = getDataFile();
        initIOHandler(ioHandler, dataFile);
        final int numRecords = ioHandler.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            getLogger().info(String.format("ingestion of record '%d/%d\'", (i + 1), numRecords));
            persistObservation(ioHandler, i);
        }
    }

    private void ingestCoincidences() {

    }

    private void ingestOnce(final Object data, String queryString) {
        final PersistenceManager persistenceManager = delegate.getPersistenceManager();
        final Query query = persistenceManager.createNativeQuery(queryString, Integer.class);
        int result = (Integer) query.getSingleResult();
        if (result == 0) {
            persistenceManager.persist(data);
        } else {
            getLogger().info("Data of type '" + data.getClass().getSimpleName() + "' already ingested.");
        }
    }

    private DataFile getDataFile() {
        final File mmdFile = getMmdFile();
        return getDataFile(mmdFile);
    }

    private DataFile getDataFile(final File file) {
        if (dataFileMap.get(file) == null) {
            final DataFile dataFile = DataUtil.createDataFile(file, dataSchema);
            dataFileMap.put(file, dataFile);
            return dataFile;
        }
        return dataFileMap.get(file);
    }

    private File getMmdFile() {
        final String filename = getConfiguration().getProperty("mms.test.arc3.output.filename", "mmd.nc");
        if (mmdFile == null) {
            mmdFile = new File(filename);
        }
        return mmdFile;
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
