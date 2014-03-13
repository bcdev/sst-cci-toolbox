package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.*;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


public class StorageImplTest {

    private static final int ID = 1234;
    private static final String GET_OBSERVATION_SQL = "select o from Observation o where o.id = ?1";

    private PersistenceManager persistenceManager;
    private StorageImpl storageImpl;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        storageImpl = new StorageImpl(persistenceManager);
    }

    @Test
    public void testGetColumn() {
        final String sql = "select c from Column c where c.name = ?1";
        final String columnName = "waite_snake";
        final Item column = new ColumnBuilder().name(columnName).build();

        when(persistenceManager.pick(sql, columnName)).thenReturn(column);

        final Column toolStorageColumn = storageImpl.getColumn(columnName);
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

        final List<? extends Item> allColumns = storageImpl.getAllColumns();
        assertNotNull(allColumns);
        assertEquals(1, allColumns.size());
        assertEquals("test_me", allColumns.get(0).getName());

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

        final List<String> namesFromStorage = storageImpl.getAllColumnNames();
        assertNotNull(namesFromStorage);

        verify(persistenceManager, times(1)).createQuery(sql);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(persistenceManager);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testStoreColumns() {
        final Column column = (Column) new ColumnBuilder().name("Hannibal").build();

        storageImpl.store(column);

        verify(persistenceManager, times(1)).persist(column);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetDataFile() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/over/the/rainbow";
        final DataFile dataFile = createDataFile(path);

        when(persistenceManager.pick(sql, path)).thenReturn(dataFile);

        final DataFile toolStorageDatafile = storageImpl.getDatafile(path);
        assertNotNull(toolStorageDatafile);
        assertEquals(path, toolStorageDatafile.getPath());

        verify(persistenceManager, times(1)).pick(sql, path);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreDataFile() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/left/of/rome";
        final DataFile dataFile = createDataFile(path);

        when(persistenceManager.pick(sql, path)).thenReturn(dataFile);

        final int storedId = storageImpl.store(dataFile);
        assertEquals(ID, storedId);

        verify(persistenceManager, times(1)).persist(dataFile);
        verify(persistenceManager, times(1)).pick(sql, path);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetObservation() {
        final int id = 8876;
        final String name = "TestObservation";
        final Observation observation = new Observation();
        observation.setName(name);

        when(persistenceManager.pick(GET_OBSERVATION_SQL, id)).thenReturn(observation);

        final Observation toolStorageObservation = storageImpl.getObservation(id);
        assertNotNull(toolStorageObservation);
        assertEquals(name, toolStorageObservation.getName());

        verify(persistenceManager, times(1)).pick(GET_OBSERVATION_SQL, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetRelatedObservation() {
        final int id = 98843;
        final String name = "related";
        final RelatedObservation observation = new RelatedObservation();
        observation.setName(name);

        when(persistenceManager.pick(GET_OBSERVATION_SQL, id)).thenReturn(observation);

        final RelatedObservation toolStorageObservation = storageImpl.getRelatedObservation(id);
        assertNotNull(toolStorageObservation);
        assertEquals(name, toolStorageObservation.getName());

        verify(persistenceManager, times(1)).pick(GET_OBSERVATION_SQL, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetReferenceObservation() {
        final String sql = "select o from ReferenceObservation o where o.id = ?1";
        final int id = 2286;
        final String name = "refer_to_me";
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setName(name);

        when(persistenceManager.pick(sql, id)).thenReturn(referenceObservation);

        final ReferenceObservation toolStorageReferenceObservation = storageImpl.getReferenceObservation(id);
        assertNotNull(toolStorageReferenceObservation);
        assertEquals(name, toolStorageReferenceObservation.getName());

        verify(persistenceManager, times(1)).pick(sql, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetSensor() {
        final String sql = "select s from Sensor s where s.name = ?1";
        final String sensorName = "blabla";
        final Sensor sensor = new SensorBuilder().name(sensorName).build();

        when(persistenceManager.pick(sql, sensorName)).thenReturn(sensor);

        final Sensor toolStorageSensor = storageImpl.getSensor(sensorName);
        assertNotNull(toolStorageSensor);
        assertEquals(sensorName, toolStorageSensor.getName());

        verify(persistenceManager, times(1)).pick(sql, sensorName);
        verifyNoMoreInteractions(persistenceManager);
    }

    @SuppressWarnings("deprecation")
    private DataFile createDataFile(String path) {
        final DataFile dataFile = new DataFile(path, new SensorBuilder().name("cloud").build());
        dataFile.setId(ID);
        return dataFile;
    }
}
