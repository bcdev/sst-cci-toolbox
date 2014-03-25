package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.Matchup;

import javax.persistence.Query;
import java.util.List;

class JpaMatchupStorage implements MatchupStorage {

    private static final String SQL_FOR_COUNT = "select count(m.id) from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
    public static final String SQL_FOR_MATCHUPS = "select m.id from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id order by r.time, r.id";

    private final PersistenceManager persistenceManager;

    JpaMatchupStorage(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public int getCount(MatchupQueryParameter parameter) {
        String queryString = SQL_FOR_COUNT;

        queryString = updateWithCondition(parameter, queryString);

        final long pattern = parameter.getPattern();
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

        final long pattern = parameter.getPattern();
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

    @SuppressWarnings("unchecked")
    @Override
    public List<Matchup> getForMmd(MatchupQueryParameter parameter) {
        final String sensorName = parameter.getSensorName();
        final String sql = getSelectMatchupSql(sensorName);

        final long pattern = parameter.getPattern();
        final String querySql = applyPatternAndCondition(sql, parameter.getCondition(), pattern);

        final Query query = persistenceManager.createNativeQuery(querySql, Matchup.class);
        query.setParameter(1, sensorName);
        query.setParameter(2, parameter.getStartDate());
        query.setParameter(3, parameter.getStopDate());
        if (pattern != 0) {
            query.setParameter(4, pattern);
        }

        return query.getResultList();
    }

    @Override
    public Matchup get(int matchupId) {
        return (Matchup) persistenceManager.pick("select m from Matchup m where m.id = ?1", matchupId);
    }

    // package access for testing only tb 2014-03-17
    static String getSelectMatchupSql(String sensorName) {
        String queryString;
        if ("history".equals(sensorName)) {
            // second part of union returns matchups that do not have a history observation and shall read in-situ from context MD
            queryString = "select u.id from (" +
                    // matchup (here coincidence) with history observation uses history file
                    "(select r.id id, f.path p, r.time t " +
                    "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and m.id = r.id " +
                    "and c.matchup_id = r.id " +
                    "and c.observation_id = o.id " +
                    "and o.sensor = ?1 " +
                    "and o.datafile_id = f.id " +
                    ") union (" +
                    // matchup without history uses file of reference observation
                    "select r.id id, f.path p, r.time t " +
                    "from mm_matchup m, mm_observation r, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and m.id = r.id " +
                    "and f.id = r.datafile_id " +
                    "and not exists ( select o.id from mm_coincidence c, mm_observation o " +
                    "where c.matchup_id = m.id " +
                    "and c.observation_id = o.id " +
                    "and o.sensor = ?1 ) " +
                    ") " +
                    "order by p, t, id) as u";

        } else if ("atsr_md".equals(sensorName) || "metop".equals(sensorName) || "avhrr_md".equals(sensorName)) {
            // second part of union introduced to access data for metop variables via refobs observation if metop is primary
            queryString = "select u.id from (" +
                    // matchup with sensor as related observation uses related observation file
                    "(select r.id id, f.path p, r.time t " +
                    "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and m.id = r.id " +
                    "and c.matchup_id = r.id " +
                    "and c.observation_id = o.id " +
                    "and o.sensor = ?1 " +
                    "and o.datafile_id = f.id " +
                    ") union (" +
                    // matchup with sensor as reference uses refobs file
                    "select r.id id, f.path p, r.time t " +
                    "from mm_matchup m, mm_observation r, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and r.sensor = ?1 " +
                    "and m.id = r.id " +
                    "and f.id = r.datafile_id) " +
                    "order by p, t, id) as u";

        } else if (!"Implicit".equals(sensorName)) {
            // satellite observations use related observation file
            queryString = "select r.id " +
                    "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and m.id = r.id " +
                    "and c.matchup_id = r.id " +
                    "and c.observation_id = o.id " +
                    "and o.sensor = ?1 " +
                    "and o.datafile_id = f.id " +
                    "order by f.path, r.time, r.id";

        } else {
            // implicit rules use reference observation file
            queryString = "select r.id " +
                    "from mm_matchup m, mm_observation r, mm_datafile f " +
                    "where r.time >= ?2 and r.time < ?3 " +
                    "and m.id = r.id " +
                    "and f.id = r.datafile_id " +
                    "order by f.path, r.time, r.id";

        }
        return queryString;
    }

    // package access for testing only tb 2014-03-18
    static String applyPatternAndCondition(String queryString, String condition, long pattern) {
        if (condition != null) {
            if (pattern != 0) {
                queryString = queryString.replaceAll("where r.time", "where pattern & ?4 = ?4 and " + condition + " and r.time");
            } else {
                queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
            }
        } else if (pattern != 0) {
            queryString = queryString.replaceAll("where r.time", "where pattern & ?4 = ?4 and r.time");
        }
        return queryString;
    }

    private String updateWithCondition(MatchupQueryParameter parameter, String queryString) {
        final String condition = parameter.getCondition();
        if (condition != null) {
            queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
        }
        return queryString;
    }
}
