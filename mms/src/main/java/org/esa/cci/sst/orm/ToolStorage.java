package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.*;

import javax.persistence.Query;
import java.util.List;

class ToolStorage implements Storage {

    private final PersistenceManager persistenceManager;

    ToolStorage(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public Column getColumn(String columnName) {
        return (Column) persistenceManager.pick("select c from Column c where c.name = ?1", columnName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Item> getAllColumns() {
        final Query query = persistenceManager.createQuery("select c from Column c order by c.name");
        return query.getResultList();
    }

    @Override
    public final DataFile getDatafile(final String path) {
        return (DataFile) persistenceManager.pick("select f from DataFile f where f.path = ?1", path);
    }

    public int store(DataFile dataFile) {
        persistenceManager.persist(dataFile);
        final DataFile picked = getDatafile(dataFile.getPath());
        return picked.getId();
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
}
