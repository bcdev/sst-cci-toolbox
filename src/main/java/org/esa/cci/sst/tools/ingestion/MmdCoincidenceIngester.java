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
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for re-ingesting coincidences from the mmd file to the database.
 *
 * @author Thomas Storm
 */
class MmdCoincidenceIngester {

    static final String GET_MATCHUP = "SELECT m " +
                                             "FROM Matchup m " +
                                             "WHERE m.id = %d";

    static final String GET_OBSERVATION_ID = "SELECT o1.id " +
                                                     "FROM mm_observation o1, mm_observation oref, mm_matchup m " +
                                                     "WHERE m.id = %d " +
                                                     "AND m.refObs_id = oref.id " +
                                                     "AND o1.time >= oref.time - '12:00:00' and o1.time < oref.time + '12:00:00' " +
                                                     "AND st_intersects(o1.location, oref.point)";

    static final String GET_OBSERVATION = "SELECT o " +
                                                  "FROM Observation o " +
                                                  "WHERE o.id = %d";
    private final MmdIngestionTool tool;

    MmdCoincidenceIngester(final MmdIngestionTool tool) {
        this.tool = tool;
    }

    void ingestCoincidences() throws ToolException {
        final int[] matchupIds = getMatchupIds();
        for (int matchupId : matchupIds) {
            ingestCoincidences(matchupId);
        }
    }

    private void ingestCoincidences(final int matchupId) {
        final List<Observation> observations = getObservations(matchupId);
        final Matchup matchup = (Matchup) getDatabaseObjectById(GET_MATCHUP, matchupId);
        for (Observation observation : observations) {
            final Coincidence coincidence = createCoincidence(matchup, observation);
            tool.getPersistenceManager().persist(coincidence);
        }
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

    private int[] getMatchupIds() throws ToolException {
        final String location = validateMmdFile();
        final NetcdfFile mmdFile = openMmdFile(location);
        final String varNameMatchupEscaped = NetcdfFile.escapeName(MmdReader.VARIABLE_NAME_MATCHUP);
        final Variable matchupVariable = mmdFile.findVariable(varNameMatchupEscaped);
        return getMatchupIds(matchupVariable);
    }

    private int[] getMatchupIds(final Variable matchupVariable) throws ToolException {
        final Dimension matchupDimension = matchupVariable.getDimension(0);
        final int[] shape = {matchupDimension.getLength()};
        final int[] origin = {0};
        return readMatchupIdsFromFile(matchupVariable, origin, shape);
    }

    int[] readMatchupIdsFromFile(final Variable matchupVariable, final int[] origin,
                                 final int[] shape) throws ToolException {
        final Array matchupIds;
        try {
            matchupIds = matchupVariable.read(origin, shape);
        } catch (Exception e) {
            throw new ToolException(
                    MessageFormat.format("Unable to read from variable ''{0}''.", MmdReader.VARIABLE_NAME_MATCHUP), e,
                    ToolException.TOOL_ERROR);
        }
        return toIntArray(matchupIds);
    }

    private int[] toIntArray(final Array array) {
        int[] result = new int[(int) array.getSize()];
        for (int i = 0; i < array.getSize(); i++) {
            result[i] = array.getInt(i);
        }
        return result;
    }

    private NetcdfFile openMmdFile(final String location) throws ToolException {
        final NetcdfFile mmdFile;
        try {
            mmdFile = NetcdfFile.open(location);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Cannot open mmd file ''{0}''.", location), e,
                                    ToolException.TOOL_ERROR);
        }
        return mmdFile;
    }

    private String validateMmdFile() throws ToolException {
        final String location = tool.getMmdFile().getAbsolutePath();
        try {
            NetcdfFile.canOpen(location);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Cannot open mmd file ''{0}''.", location), e,
                                    ToolException.TOOL_ERROR);
        }
        return location;
    }

    @SuppressWarnings({"unchecked"})
    List<Observation> getObservations(final int matchupId) {
        final String queryString = String.format(GET_OBSERVATION_ID, matchupId);
        final Query query = tool.getPersistenceManager().createNativeQuery(queryString);
        return getObservationsForIds((List<Integer>) query.getResultList());
    }

    private List<Observation> getObservationsForIds(final List<Integer> observationIds) {
        List<Observation> observations = new ArrayList<Observation>();
        for (int observationId : observationIds) {
            final Observation observation = (Observation) getDatabaseObjectById(GET_OBSERVATION, observationId);
            observations.add(observation);
        }
        return observations;
    }

    Object getDatabaseObjectById(final String baseQuery, final int id) {
        final String queryString = String.format(baseQuery, id);
        final Query query = tool.getPersistenceManager().createQuery(queryString);
        return query.getSingleResult();
    }

}
