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

package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;

import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Some data base queries.
 *
 * @author Ralf Quast
 */
public class Queries {

    private Queries() {
    }

    public static final String QUERY_STRING_SELECT_ALL_COLUMNS =
            "select c" +
            " from Column c" +
            " order by c.name";

    public static final String QUERY_STRING_COUNT_MATCHUPS =
            "select count(m)" +
            " from Matchup m" +
            " where m.refObs.time >= ?1 and m.refObs.time < ?2";

    public static final String QUERY_STRING_COUNT_MATCHUPS_FOR_SENSOR =
            "select count(m.id)" +
            " from mm_matchup m, mm_observation o" +
            " where o.id = m.refobs_id" +
            " and o.time >= ?1 and o.time < ?2" +
            " and m.pattern & ?3 = ?3";

    public static final String QUERY_STRING_COUNT_OBSERVATIONS =
            "select count(o)" +
            " from Observation o";

    public static final String QUERY_STRING_SELECT_MATCHUPS =
            "select m" +
            " from Matchup m" +
            " where m.refObs.time >= ?1 and m.refObs.time < ?2" +
            " order by m.refObs.time";

    public static final String QUERY_STRING_SELECT_MATCHUPS_FOR_SENSOR =
            "select m.id" +
            " from mm_matchup m, mm_observation o" +
            " where o.id = m.refobs_id" +
            " and o.time >= ?1 and o.time < ?2" +
            " and m.pattern & ?3 = ?3" +
            " and o.referenceflag <> ?4 " +
            " order by o.time";

    public static final String QUERY_STRING_SELECT_REFERENCE_OBSERVATION_FOR_MATCHUP =
            "select m.refObs" +
            " from Matchup m" +
            " where m.id = ?1";

    public static final String QUERY_STRING_SELECT_MATCHUP =
            "select m" +
            " from Matchup m" +
            " where m.id = ?1";

    @SuppressWarnings({"unchecked"})
    public static List<? extends Item> getAllColumns(PersistenceManager pm) {
        return pm.createQuery(QUERY_STRING_SELECT_ALL_COLUMNS).getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Coincidence> getCoincidences(PersistenceManager pm, int matchupId) {
        final Query query = pm.createQuery(QUERY_STRING_SELECT_MATCHUP);
        query.setParameter(1, matchupId);

        final Matchup matchup = (Matchup) query.getSingleResult();
        return matchup.getCoincidences();
    }

    public static int getMatchupCount(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(QUERY_STRING_COUNT_MATCHUPS);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);

        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount.intValue();
    }

    public static int getMatchupCount(PersistenceManager pm, Date startDate, Date stopDate, long pattern) {
        final Query query = pm.createNativeQuery(QUERY_STRING_COUNT_MATCHUPS_FOR_SENSOR);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        query.setParameter(3, pattern);

        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(QUERY_STRING_SELECT_MATCHUPS);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);

        return query.getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate, long targetPattern, long duplicateFlag) {
        final Query query = pm.createNativeQuery(QUERY_STRING_SELECT_MATCHUPS_FOR_SENSOR, Matchup.class);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        query.setParameter(3, targetPattern);
        query.setParameter(4, duplicateFlag);

        return query.getResultList();
    }

    public static int getObservationCount(PersistenceManager pm) {
        final Query query = pm.createQuery(QUERY_STRING_COUNT_OBSERVATIONS);
        final Number observationCount = (Number) query.getSingleResult();

        return observationCount.intValue();
    }

    public static ReferenceObservation getReferenceObservationForMatchup(PersistenceManager pm, int matchupId) {
        final Query query = pm.createQuery(QUERY_STRING_SELECT_REFERENCE_OBSERVATION_FOR_MATCHUP);
        query.setParameter(1, matchupId);

        return (ReferenceObservation) query.getSingleResult();
    }
}
