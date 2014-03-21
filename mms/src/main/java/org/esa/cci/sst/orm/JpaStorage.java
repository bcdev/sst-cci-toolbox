package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.*;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Query;
import java.util.Date;
import java.util.List;

class JpaStorage implements Storage {

    // rq-20140217 - do not delete, might be useful later
//    private static final String COINCIDING_OBSERVATION_QUERY_TEMPLATE_STRING =
//            "select o.id"
//            + " from mm_observation o"
//            + " where o.sensor = ?1"
//            + " and o.time >= timestamp ?2 - interval '420:00:00' and o.time < timestamp ?2 + interval '420:00:00'"
//            + " and st_intersects(o.location, st_geomfromewkt(?3))"
//            + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";


    private static final String SENSOR_OBSERVATION_QUERY_TEMPLATE_STRING =
            "select o.id"
                    + " from mm_observation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= timestamp ?2 and o.time < timestamp ?3"
                    + " order by o.time, o.id";

    private final PersistenceManager persistenceManager;

    JpaStorage(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public DataFile getDatafile(int id) {
        return (DataFile) persistenceManager.pick("select f from DataFile f where f.id = ?1", id);
    }

    @Override
    public final DataFile getDatafile(final String path) {
        return (DataFile) persistenceManager.pick("select f from DataFile f where f.path = ?1", path);
    }

    @Override
    public DataFile getDatafileWithTransaction(String path) {
        try {
            persistenceManager.transaction();
            final DataFile dataFile = getDatafile(path);
            persistenceManager.commit();

            return dataFile;
        } catch (Exception e) {
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }
    }

    public void store(DataFile dataFile) {
        persistenceManager.persist(dataFile);
    }

    @Override
    public int storeWithTransaction(DataFile dataFile) {
        try {
            persistenceManager.transaction();
            persistenceManager.persist(dataFile);
            persistenceManager.commit();
        } catch (Exception e) {
            persistenceManager.rollback();
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }

        try {
            persistenceManager.transaction();
            final DataFile picked = getDatafile(dataFile.getPath());
            persistenceManager.commit();
            return picked.getId();
        } catch (Exception e) {
            persistenceManager.rollback();
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }
    }

    @Override
    public Observation getObservation(int id) {
        return (Observation) persistenceManager.pick("select o from Observation o where o.id = ?1", id);
    }

    @Override
    public RelatedObservation getRelatedObservation(int id) {
        return (RelatedObservation) getObservation(id);
    }

    @Override
    public ReferenceObservation getReferenceObservation(int id) {
        return (ReferenceObservation) persistenceManager.pick("select o from ReferenceObservation o where o.id = ?1", id);
    }

    @Override
    public final Sensor getSensor(final String sensorName) {
        return (Sensor) persistenceManager.pick("select s from Sensor s where s.name = ?1", sensorName);
    }

    @Override
    public List<RelatedObservation> getRelatedObservations(String sensorName, Date startDate, Date stopDate) {
        final String s1 = TimeUtil.formatCcsdsUtcFormat(startDate);
        final String s2 = TimeUtil.formatCcsdsUtcFormat(stopDate);
        final String queryString = SENSOR_OBSERVATION_QUERY_TEMPLATE_STRING
                .replaceAll("\\?2", "'" + s1 + "'")
                .replaceAll("\\?3", "'" + s2 + "'");
        final Query query = persistenceManager.createNativeQuery(queryString, RelatedObservation.class);
        query.setParameter(1, sensorName);

        //noinspection unchecked
        return query.getResultList();
    }

    @Override
    public void store(InsituObservation insituObservation) {
        persistenceManager.persist(insituObservation);
    }
}
