package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.Item;

import javax.persistence.Query;
import java.util.List;

class JpaColumnStorage implements ColumnStorage {

    private final PersistenceManager persistenceManager;

    JpaColumnStorage(PersistenceManager persistenceManager) {
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

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getAllColumnNames() {
        final Query query = persistenceManager.createQuery("select c.name from Column c");
        return query.getResultList();
    }

    public void store(Column column) {
        persistenceManager.persist(column);
    }

    @Override
    public void deleteAll() {
        final Query query = persistenceManager.createQuery("delete from Column c");
        query.executeUpdate();
    }
}
