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

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.tools.ErrorHandler;
import org.esa.cci.sst.tools.ToolException;

import java.text.MessageFormat;

/**
 * Responsible for re-ingesting observations from an mmd file into the database.
 *
 * @author Thomas Storm
 */
class MmdObservationIngester {

    private final MmdIngestionTool tool;

    MmdObservationIngester(final MmdIngestionTool tool) {
        this.tool = tool;
    }

    void ingestObservations() throws ToolException {
        final IOHandler ioHandler = tool.getIoHandler();
        final int numRecords = ioHandler.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            tool.getLogger().info(String.format("ingestion of record '%d/%d\'", (i + 1), numRecords));
            persistObservation(ioHandler, i);
        }
    }

    private void persistObservation(final IOHandler ioHandler, int recordNo) throws ToolException {
        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        persistenceManager.transaction();
        try {
            tool.getDelegate().persistObservation(ioHandler, recordNo);
        } catch (Exception e) {
            final ErrorHandler errorHandler = tool.getErrorHandler();
            errorHandler.handleError(e, MessageFormat.format("Error persisting observation ''{0}''.", recordNo),
                                     ToolException.TOOL_ERROR);
        } finally {
            persistenceManager.commit();
        }
    }

}
