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

import org.esa.cci.sst.data.Column;
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

    public static final String SELECT_ALL_COLUMNS_QUERY_STRING =
            "select c" +
            " from Column c" +
            " order by c.name";

    public static final String COUNT_MATCHUPS_QUERY_STRING =
            "select count(m)" +
            " from Matchup m"
            + " where m.refObs.time >= ?1 and m.refObs.time < ?2";

    public static final String SELECT_MATCHUPS_QUERY_STRING =
            "select m" +
            " from Matchup m"
            + " where m.refObs.time >= ?1 and m.refObs.time < ?2"
            + " order by m.refObs.time";

    public static int getMatchupCount(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(COUNT_MATCHUPS_QUERY_STRING);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount == null ? 0 : matchupCount.intValue();
    }

    public static List<Column> getAllColumns(PersistenceManager pm) {
        //noinspection unchecked
        return pm.createQuery(SELECT_ALL_COLUMNS_QUERY_STRING).getResultList();
    }

    public static List<Matchup> getMatchups(PersistenceManager pm, Date startDate, Date stopDate) {
        final Query query = pm.createQuery(SELECT_MATCHUPS_QUERY_STRING);
        query.setParameter(1, startDate);
        query.setParameter(2, stopDate);
        //noinspection unchecked
        return query.getResultList();
    }
}
