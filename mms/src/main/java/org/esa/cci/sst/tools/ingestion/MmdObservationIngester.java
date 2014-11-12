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

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Timeable;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.util.TimeUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

/**
 * Responsible for re-ingesting observations and coincidences from an mmd file into the database.
 *
 * @author Thomas Storm
 */
class MmdObservationIngester {

    private final BasicTool tool;
    private final Ingester ingester;
    private final MmdReader reader;
    private final long pattern;

    MmdObservationIngester(BasicTool tool, Ingester ingester, MmdReader reader, long pattern) {
        this.tool = tool;
        this.ingester = ingester;
        this.reader = reader;
        this.pattern = pattern;
    }

    void ingestObservations() {
        final int numRecords = reader.getNumRecords();
        for (int i = 0; i < numRecords; i++) {
            SstLogging.getLogger().fine(String.format("ingestion of record '%d/%d\'", (i + 1), numRecords));
            persistObservation(reader, i);
        }
    }

    private void persistObservation(final MmdReader reader, int recordNo) {
        try {
            final Observation observation = reader.readObservation(recordNo);
            final boolean persisted = ingester.persistObservation(observation, recordNo);
            if (persisted) {
                persistCoincidence(reader, recordNo, observation);
            }
        } catch (Exception e) {
            final String message = MessageFormat.format("Error persisting observation ''{0}''.", recordNo + 1);
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    private void persistCoincidence(final MmdReader reader, final int recordNo,
                                    final Observation observation) throws IOException {
        final int matchupId = reader.getMatchupId(recordNo);
        final Matchup matchup = tool.getPersistenceManager().getMatchupStorage().get(matchupId);
        if (matchup == null) {
            return;
        }
        final Coincidence coincidence = createCoincidence(matchup, observation);
        tool.getPersistenceManager().persist(coincidence);
        matchup.setPattern(matchup.getPattern() | pattern);
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
        if (observation instanceof Timeable) {
            final Date matchupTime = matchup.getRefObs().getTime();
            final Date observationTime = ((Timeable) observation).getTime();
            final double timeDelta = TimeUtil.getTimeDifferenceInSeconds(matchupTime, observationTime);
            coincidence.setTimeDifference(timeDelta);
        }
    }

}
