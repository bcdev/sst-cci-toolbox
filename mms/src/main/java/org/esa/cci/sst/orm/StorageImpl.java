package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.*;

class StorageImpl implements Storage {

    private final PersistenceManager persistenceManager;

    StorageImpl(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
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
