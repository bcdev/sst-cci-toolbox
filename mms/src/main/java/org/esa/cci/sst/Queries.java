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
import org.esa.cci.sst.tools.ToolException;

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

    public static final String QUERY_STRING_COUNT_MATCHUPS_FOR_SENSOR =
            "select count(m.id)" +
            " from mm_matchup m, mm_observation r" +
            " where r.time >= ?1 and r.time < ?2" +
            " and r.id = m.refobs_id";

    public static final String QUERY_STRING_SELECT_MATCHUPS_FOR_SENSOR =
            "select m.id" +
            " from mm_matchup m, mm_observation r" +
            " where r.time >= ?1 and r.time < ?2" +
            " and r.id = m.refobs_id" +
            " order by r.time, r.id";

    @SuppressWarnings({"unchecked"})
    public static List<? extends Item> getAllColumns(PersistenceManager pm) {
        return pm.createQuery(QUERY_STRING_SELECT_ALL_COLUMNS).getResultList();
    }

    public static int getMatchupCount(PersistenceManager pm, Date startDate, Date stopDate, String condition, int pattern) {
        String queryString = QUERY_STRING_COUNT_MATCHUPS_FOR_SENSOR;
        if (condition != null) {
            queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
        }
        if (pattern != 0) {
            queryString = queryString.replaceAll(" where ", " where m.pattern & ?3 = ?3 and ");
        }
        final Query query = pm.createNativeQuery(queryString);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        if (pattern != 0) {
            query.setParameter(3, pattern);
        }

        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate, String condition, int pattern) {
        String queryString = QUERY_STRING_SELECT_MATCHUPS_FOR_SENSOR;
        if (condition != null) {
            queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
        }
        if (pattern != 0) {
            queryString = queryString.replaceAll("order by", "and m.pattern & ?3 = ?3 order by");
        }
        final Query query = pm.createNativeQuery(queryString, Matchup.class);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        if (pattern != 0) {
            query.setParameter(3, pattern);
        }

        return query.getResultList();
    }
}
