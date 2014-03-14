package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.Matchup;

import javax.persistence.Query;
import java.util.List;

class MatchupStorageImpl implements MatchupStorage {

    private static final String SQL_FOR_COUNT = "select count(m.id) from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
    public static final String SQL_FOR_MATCHUPS = "select m.id from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id order by r.time, r.id";

    private final PersistenceManager persistenceManager;

    MatchupStorageImpl(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public int getCount(MatchupQueryParameter parameter) {
        String queryString = SQL_FOR_COUNT;

        queryString = updateWithCondition(parameter, queryString);

        final int pattern = parameter.getPattern();
        if (pattern != 0) {
            queryString = queryString.replaceAll(" where ", " where m.pattern & ?3 = ?3 and ");
        }

        final Query query = persistenceManager.createNativeQuery(queryString);
        query.setParameter(1, parameter.getStartDate());
        query.setParameter(2, parameter.getStopDate());
        if (pattern != 0) {
            query.setParameter(3, pattern);
        }

        final Number matchupCount = (Number) query.getSingleResult();
        return matchupCount.intValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Matchup> get(MatchupQueryParameter parameter) {
        String queryString = SQL_FOR_MATCHUPS;

        queryString = updateWithCondition(parameter, queryString);

        final int pattern = parameter.getPattern();
        if (pattern != 0) {
            queryString = queryString.replaceAll("order by", "and m.pattern & ?3 = ?3 order by");
        }

        final Query query = persistenceManager.createNativeQuery(queryString, Matchup.class);
        query.setParameter(1, parameter.getStartDate());
        query.setParameter(2, parameter.getStopDate());
        if (pattern != 0) {
            query.setParameter(3, pattern);
        }

        return query.getResultList();
    }

    @Override
    public Matchup get(int matchupId) {
        return (Matchup) persistenceManager.pick("select m from Matchup m where m.id = ?1", matchupId);
    }

    private String updateWithCondition(MatchupQueryParameter parameter, String queryString) {
        final String condition = parameter.getCondition();
        if (condition != null) {
            queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
        }
        return queryString;
    }
}
