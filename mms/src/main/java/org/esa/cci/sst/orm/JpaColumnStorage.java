package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.ToolException;

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

    @Override
    public List<Item> getAllColumnsWithTransaction() {
        try {
            persistenceManager.transaction();
            final List<Item> allColumns = getAllColumns();
            persistenceManager.commit();
            return allColumns;
        } catch (Exception e) {
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getAllColumnNames() {
        final Query query = persistenceManager.createQuery("select c.name from Column c");
        return query.getResultList();
    }

    @Override
    public List<String> getAllColumnNamesWithTransaction() {
        try {
            persistenceManager.transaction();
            final List<String> columnNames = getAllColumnNames();
            persistenceManager.commit();
            return columnNames;
        } catch (Exception e) {
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }
    }

    public void store(Column column) {
        persistenceManager.persist(column);
    }

    @Override
    public void storeWithTransaction(Column column) {
        try {
            persistenceManager.transaction();
            store(column);
            persistenceManager.commit();
        } catch (Exception e) {
            e.printStackTrace();
            persistenceManager.rollback();
            throw new ToolException("Database error", e, ToolException.TOOL_DB_ERROR);
        }
    }

    @Override
    public void deleteAll() {
        final Query query = persistenceManager.createQuery("delete from Column c");
        query.executeUpdate();
    }
}
