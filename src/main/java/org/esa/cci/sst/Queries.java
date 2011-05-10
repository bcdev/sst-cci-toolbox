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

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
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
            " order by o.time";

    public static int getMatchupCount(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(QUERY_STRING_COUNT_MATCHUPS);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public static List<? extends Item> getAllColumns(PersistenceManager pm) {
        return pm.createQuery(QUERY_STRING_SELECT_ALL_COLUMNS).getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(QUERY_STRING_SELECT_MATCHUPS);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        return query.getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate, long pattern) {
        final Query query = pm.createNativeQuery(QUERY_STRING_SELECT_MATCHUPS_FOR_SENSOR, Matchup.class);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        query.setParameter(3, pattern);
        return query.getResultList();
    }
}
