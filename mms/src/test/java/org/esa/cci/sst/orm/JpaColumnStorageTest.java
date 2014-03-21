package org.esa.cci.sst.orm;


import org.apache.openjpa.persistence.PersistenceException;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.ToolException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class JpaColumnStorageTest {

    private PersistenceManager persistenceManager;
    private JpaColumnStorage columnStorage;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        columnStorage = new JpaColumnStorage(persistenceManager);
    }

    @Test
    public void testInterfaceIsImplemented() {
        assertThat(columnStorage, is(instanceOf(ColumnStorage.class)));
    }

    @Test
    public void testGetColumn() {
        final String sql = "select c from Column c where c.name = ?1";
        final String columnName = "waite_snake";
        final Item column = new ColumnBuilder().name(columnName).build();

        when(persistenceManager.pick(sql, columnName)).thenReturn(column);

        final Column toolStorageColumn = columnStorage.getColumn(columnName);
        assertNotNull(toolStorageColumn);
        assertEquals(columnName, toolStorageColumn.getName());

        verify(persistenceManager, times(1)).pick(sql, columnName);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetAllColumns() {
        final String sql = "select c from Column c order by c.name";
        final ArrayList<Column> columnList = new ArrayList<>();
        columnList.add((Column) new ColumnBuilder().name("test_me").build());

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(columnList);

        final List<? extends Item> allColumns = columnStorage.getAllColumns();
        assertNotNull(allColumns);
        assertEquals(1, allColumns.size());
        assertEquals("test_me", allColumns.get(0).getName());

        verify(persistenceManager, times(1)).createQuery(sql);
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetAllColumnsWithTransaction() {
        final String sql = "select c from Column c order by c.name";
        final ArrayList<Column> columnList = new ArrayList<>();
        columnList.add((Column) new ColumnBuilder().name("test_me").build());

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(columnList);

        final List<? extends Item> allColumns = columnStorage.getAllColumnsWithTransaction();
        assertNotNull(allColumns);
        assertEquals(1, allColumns.size());
        assertEquals("test_me", allColumns.get(0).getName());

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).createQuery(sql);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetAllColumnsWithTransaction_fails() {
        final String sql = "select c from Column c order by c.name";

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        doThrow(new PersistenceException(null, null, null, true)).when(query).getResultList();

        try {
            columnStorage.getAllColumnsWithTransaction();
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).createQuery(sql);
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetAllColumnNames() {
        final String sql = "select c.name from Column c";
        final ArrayList<String> columnNameList = new ArrayList<>();
        columnNameList.add("Johanna");
        columnNameList.add("Uwe");

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(columnNameList);

        final List<String> namesFromStorage = columnStorage.getAllColumnNames();
        assertNotNull(namesFromStorage);

        verify(persistenceManager, times(1)).createQuery(sql);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(persistenceManager);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetAllColumnNamesWithTransaction() {
        final String sql = "select c.name from Column c";
        final ArrayList<String> columnNameList = new ArrayList<>();
        columnNameList.add("Johanna");
        columnNameList.add("Uwe");

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(columnNameList);

        final List<String> namesFromStorage = columnStorage.getAllColumnNamesWithTransaction();
        assertNotNull(namesFromStorage);

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).createQuery(sql);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetAllColumnNamesWithTransaction_failure() {
        final String sql = "select c.name from Column c";

        final Query query = mock(Query.class);

        when(persistenceManager.createQuery(sql)).thenReturn(query);
        doThrow(new PersistenceException(null, null, null, true)).when(query).getResultList();

        try {
            columnStorage.getAllColumnNamesWithTransaction();
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).createQuery(sql);
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testStoreColumn() {
        final Column column = (Column) new ColumnBuilder().name("Hannibal").build();

        columnStorage.store(column);

        verify(persistenceManager, times(1)).persist(column);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreColumnWithTransaction() {
        final Column column = (Column) new ColumnBuilder().name("Schneckchen").build();

        columnStorage.storeWithTransaction(column);

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).persist(column);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreColumnWithTransaction_failure() {
        final Column column = (Column) new ColumnBuilder().name("Gansta-Rapper").build();

        doThrow(new PersistenceException(null, null, null, true)).when(persistenceManager).persist(column);

        try {
            columnStorage.storeWithTransaction(column);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).persist(column);
        verify(persistenceManager, times(1)).rollback();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testDeleteAllColumns() {
        final String sql = "delete from Column c";

        final Query query = mock(Query.class);
        when(persistenceManager.createQuery(sql)).thenReturn(query);

        columnStorage.deleteAll();

        verify(persistenceManager, times(1)).createQuery(sql);
        verifyNoMoreInteractions(persistenceManager);
        verify(query, times(1)).executeUpdate();
        verifyNoMoreInteractions(query);
    }
}
