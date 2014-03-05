package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


public class ToolStorageTest {

    private static final int ID = 1234;
    private static final String GET_OBSERVATION_SQL = "select o from Observation o where o.id = ?1";

    private PersistenceManager persistenceManager;
    private ToolStorage toolStorage;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        toolStorage = new ToolStorage(persistenceManager);
    }

    @Test
    public void testGetColumn() {
        final String sql = "select c from Column c where c.name = ?1";
        final String columnName = "waite_snake";
        final Item column = new ColumnBuilder().name(columnName).build();

        when(persistenceManager.pick(sql, columnName)).thenReturn(column);

        final Column toolStorageColumn = toolStorage.getColumn(columnName);
        assertNotNull(toolStorageColumn);
        assertEquals(columnName, toolStorageColumn.getName());

        verify(persistenceManager, times(1)).pick(sql, columnName);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetDataFile() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/over/the/rainbow";
        final DataFile dataFile = createDataFile(path);

        when(persistenceManager.pick(sql, path)).thenReturn(dataFile);

        final DataFile toolStorageDatafile = toolStorage.getDatafile(path);
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

        final int storedId = toolStorage.store(dataFile);
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

        final Observation toolStorageObservation = toolStorage.getObservation(id);
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

        final RelatedObservation toolStorageObservation = toolStorage.getRelatedObservation(id);
        assertNotNull(toolStorageObservation);
        assertEquals(name, toolStorageObservation.getName());

        verify(persistenceManager, times(1)).pick(GET_OBSERVATION_SQL, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetSensor() {
        final String sql = "select s from Sensor s where s.name = ?1";
        final String sensorName = "blabla";
        final Sensor sensor = new SensorBuilder().name(sensorName).build();

        when(persistenceManager.pick(sql, sensorName)).thenReturn(sensor);

        final Sensor toolStorageSensor = toolStorage.getSensor(sensorName);
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
