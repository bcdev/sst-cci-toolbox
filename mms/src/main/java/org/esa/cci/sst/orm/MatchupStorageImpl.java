package org.esa.cci.sst.orm;

import javax.persistence.Query;

class MatchupStorageImpl implements MatchupStorage {

    private static final String SQL_FOR_COUNT ="select count(m.id) from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";

    private final PersistenceManager persistenceManager;

    MatchupStorageImpl(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public int getCount(MatchupQueryParameter parameter) {
        String queryString = SQL_FOR_COUNT;

        final String condition = parameter.getCondition();
        if (condition != null) {
            queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
        }

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
}
