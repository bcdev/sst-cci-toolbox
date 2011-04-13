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

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.ErrorHandler;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Responsible for re-ingesting observations and coincidences from an mmd file into the database.
 *
 * @author Thomas Storm
 */
class MmdObservationIngester {

    private final MmdIngester ingester;

    static final String GET_MATCHUP = "SELECT m " +
                                      "FROM Matchup m " +
                                      "WHERE m.id = %d";

    MmdObservationIngester(final MmdIngester ingester) {
        this.ingester = ingester;
    }

    void ingestObservations() throws ToolException {
        final MmdReader ioHandler = (MmdReader) ingester.getIoHandler();
        final int numRecords = ioHandler.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            ingester.getLogger().info(String.format("ingestion of record '%d/%d\'", (i + 1), numRecords));
            persistObservation(ioHandler, i);
        }
    }

    private void persistObservation(final MmdReader ioHandler, int recordNo) throws ToolException {
        final PersistenceManager persistenceManager = ingester.getPersistenceManager();
        persistenceManager.transaction();
        try {
            final Observation observation = ioHandler.readObservation(recordNo);
            persistCoincidence(ioHandler, recordNo, observation);
            ingester.getDelegate().persistObservation(observation, recordNo);
        } catch (Exception e) {
            final ErrorHandler errorHandler = ingester.getErrorHandler();
            errorHandler.handleError(e, MessageFormat.format("Error persisting observation ''{0}''.", recordNo),
                                     ToolException.TOOL_ERROR);
        } finally {
            persistenceManager.commit();
        }
    }

    private void persistCoincidence(final MmdReader ioHandler, final int recordNo, final Observation observation) throws
                                                                                                                  IOException {
        final int matchupId = ioHandler.getMatchupId(recordNo);
        final Matchup matchup = (Matchup) getDatabaseObjectById(GET_MATCHUP, matchupId);
        final Coincidence coincidence = createCoincidence(matchup, observation);
        ingester.getPersistenceManager().persist(coincidence);
    }

    private Coincidence createCoincidence(final Matchup matchup, final Observation observation) {
        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(observation);
        setCoincidenceTimeDelta(matchup, observation, coincidence);
        return coincidence;
    }

    private void setCoincidenceTimeDelta(final Matchup matchup, final Observation observation,
                                         final Coincidence coincidence) {
        final int timeDelta = TimeUtil.computeTimeDelta(matchup, observation);
        coincidence.setTimeDifference(timeDelta);
    }

    private Object getDatabaseObjectById(final String baseQuery, final int id) {
        final String queryString = String.format(baseQuery, id);
        final Query query = ingester.getPersistenceManager().createQuery(queryString);
        return query.getSingleResult();
    }

}
