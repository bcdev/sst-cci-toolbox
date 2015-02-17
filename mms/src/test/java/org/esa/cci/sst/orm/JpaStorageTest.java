package org.esa.cci.sst.orm;

import org.apache.openjpa.persistence.PersistenceException;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class JpaStorageTest {

    private static final int ID = 1234;
    private static final String GET_OBSERVATION_SQL = "select o from Observation o where o.id = ?1";

    private PersistenceManager persistenceManager;
    private JpaStorage jpaStorage;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        jpaStorage = new JpaStorage(persistenceManager);
    }

    @Test
    public void testGetDataFile() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/over/the/rainbow";
        final DataFile dataFile = createDataFile(path);

        when(persistenceManager.pick(sql, path)).thenReturn(dataFile);

        final DataFile toolStorageDatafile = jpaStorage.getDatafile(path);
        assertNotNull(toolStorageDatafile);
        assertEquals(path, toolStorageDatafile.getPath());

        verify(persistenceManager, times(1)).pick(sql, path);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetDataFileWithTransaction() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/over/the/rainbow";
        final DataFile dataFile = createDataFile(path);

        when(persistenceManager.pick(sql, path)).thenReturn(dataFile);

        final DataFile toolStorageDatafile = jpaStorage.getDatafileWithTransaction(path);
        assertNotNull(toolStorageDatafile);
        assertEquals(path, toolStorageDatafile.getPath());

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).pick(sql, path);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetDataFileWithTransaction_fails() {
        final String sql = "select f from DataFile f where f.path = ?1";
        final String path = "/over/the/rainbow";

        doThrow(new PersistenceException(null, null, null, true)).when(persistenceManager).pick(sql, path);

        try {
            jpaStorage.getDatafileWithTransaction(path);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).pick(sql, path);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetDataFile_ById() {
        final String sql = "select f from DataFile f where f.id = ?1";
        final int id = 23;
        final DataFile dataFile = createDataFile(id);

        when(persistenceManager.pick(sql, id)).thenReturn(dataFile);

        final DataFile toolStorageDatafile = jpaStorage.getDatafile(id);
        assertNotNull(toolStorageDatafile);
        assertEquals(id, toolStorageDatafile.getId());
        assertEquals("something", toolStorageDatafile.getPath());

        verify(persistenceManager, times(1)).pick(sql, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreDataFile() {
        final String path = "/left/of/rome";
        final DataFile dataFile = createDataFile(path);

        jpaStorage.store(dataFile);

        verify(persistenceManager, times(1)).persist(dataFile);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetObservation() {
        final int id = 8876;
        final String name = "TestObservation";
        final Observation observation = new Observation();
        observation.setName(name);

        when(persistenceManager.pick(GET_OBSERVATION_SQL, id)).thenReturn(observation);

        final Observation toolStorageObservation = jpaStorage.getObservation(id);
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

        final RelatedObservation toolStorageObservation = jpaStorage.getRelatedObservation(id);
        assertNotNull(toolStorageObservation);
        assertEquals(name, toolStorageObservation.getName());

        verify(persistenceManager, times(1)).pick(GET_OBSERVATION_SQL, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetRelatedObservations() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-01-01T13:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-01-05T17:00:00Z");
        final String sensorName = "thermometer";
        final String sql = "select o.id from mm_observation o where o.sensor = ?1 and o.time >= timestamp '2010-01-01T13:00:00Z' and o.time < timestamp '2010-01-05T17:00:00Z' order by o.time, o.id";

        final List<RelatedObservation> observations = new ArrayList<>();
        final RelatedObservation observation = new RelatedObservation();
        observation.setName("tested thing");
        observations.add(observation);

        final Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(observations);
        when(persistenceManager.createNativeQuery(sql, RelatedObservation.class)).thenReturn(query);

        final List<RelatedObservation> storedObservations = jpaStorage.getRelatedObservations(sensorName, startDate, stopDate);
        assertNotNull(storedObservations);
        assertEquals(1, storedObservations.size());

        verify(persistenceManager, times(1)).createNativeQuery(sql, RelatedObservation.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetRelatedObservationsOrderedByTimeDelta() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-01-01T13:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-01-05T17:00:00Z");
        final Date referenceDate = TimeUtil.parseCcsdsUtcFormat("2010-01-03T15:00:00Z");
        final String sensorName = "odometer";
        final String sql = "select o.id from mm_observation o where o.sensor = ?1 and o.time >= timestamp '2010-01-01T13:00:00Z' and o.time < timestamp '2010-01-05T17:00:00Z' order by abs(extract(epoch from o.time) - extract(epoch from timestamp '2010-01-03T15:00:00Z'))";

        final List<RelatedObservation> observations = new ArrayList<>();
        final RelatedObservation observation = new RelatedObservation();
        observation.setName("tested observation");
        observations.add(observation);

        final Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(observations);
        when(persistenceManager.createNativeQuery(sql, RelatedObservation.class)).thenReturn(query);

        final List<RelatedObservation> storedObservations = jpaStorage.getRelatedObservationsOrderedByTimeDelta(sensorName, startDate, stopDate, referenceDate);
        assertNotNull(storedObservations);
        assertEquals(1, storedObservations.size());

        verify(persistenceManager, times(1)).createNativeQuery(sql, RelatedObservation.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetGlobalObservations() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2011-02-01T13:00:00Z");
        final Date stoptDate = TimeUtil.parseCcsdsUtcFormat("2011-02-05T17:00:00Z");
        final String sensorName = "Zollstock";
        final String sql = "select o.id from mm_observation o where o.sensor = ?1 and o.time >= timestamp '2011-02-01T13:00:00Z' and o.time < timestamp '2011-02-05T17:00:00Z' order by o.time, o.id";

        final List<GlobalObservation> observations = new ArrayList<>();
        final GlobalObservation observation = new GlobalObservation();
        observation.setName("tested thing");
        observations.add(observation);

        final Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(observations);
        when(persistenceManager.createNativeQuery(sql, GlobalObservation.class)).thenReturn(query);

        final List<GlobalObservation> storedObservations = jpaStorage.getGlobalObservations(sensorName, startDate, stoptDate);
        assertNotNull(storedObservations);
        assertEquals(1, storedObservations.size());

        verify(persistenceManager, times(1)).createNativeQuery(sql, GlobalObservation.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetGlobalObservationsOrderedByTimeDelta() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2011-02-01T13:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2011-02-05T17:00:00Z");
        final Date referenceDate = TimeUtil.parseCcsdsUtcFormat("2011-02-03T15:00:00Z");
        final String sensorName = "BangBüx";
        final String sql = "select o.id from mm_observation o where o.sensor = ?1 and o.time >= timestamp '2011-02-01T13:00:00Z' and o.time < timestamp '2011-02-05T17:00:00Z' order by abs(extract(epoch from o.time) - extract(epoch from timestamp '2011-02-03T15:00:00Z'))";

        final List<GlobalObservation> observations = new ArrayList<>();
        final GlobalObservation observation = new GlobalObservation();
        observation.setName("tested GlobObs");
        observations.add(observation);

        final Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(observations);
        when(persistenceManager.createNativeQuery(sql, GlobalObservation.class)).thenReturn(query);

        final List<GlobalObservation> storedObservations = jpaStorage.getGlobalObservationsOrderedByTimeDelta(sensorName, startDate, stopDate, referenceDate);
        assertNotNull(storedObservations);
        assertEquals(1, storedObservations.size());

        verify(persistenceManager, times(1)).createNativeQuery(sql, GlobalObservation.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetReferenceObservation() {
        final String sql = "select o from ReferenceObservation o where o.id = ?1";
        final int id = 2286;
        final String name = "refer_to_me";
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setName(name);

        when(persistenceManager.pick(sql, id)).thenReturn(referenceObservation);

        final ReferenceObservation toolStorageReferenceObservation = jpaStorage.getReferenceObservation(id);
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

        final Sensor toolStorageSensor = jpaStorage.getSensor(sensorName);
        assertNotNull(toolStorageSensor);
        assertEquals(sensorName, toolStorageSensor.getName());

        verify(persistenceManager, times(1)).pick(sql, sensorName);
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetSensorWithTransaction() {
        final String sql = "select s from Sensor s where s.name = ?1";
        final String sensorName = "blabla";
        final Sensor sensor = new SensorBuilder().name(sensorName).build();

        when(persistenceManager.pick(sql, sensorName)).thenReturn(sensor);

        final Sensor toolStorageSensor = jpaStorage.getSensorWithTransaction(sensorName);
        assertNotNull(toolStorageSensor);
        assertEquals(sensorName, toolStorageSensor.getName());

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).pick(sql, sensorName);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testGetSensorWithTransaction_failure() {
        final String sql = "select s from Sensor s where s.name = ?1";
        final String sensorName = "blabla";

        doThrow(new PersistenceException(null, null, null, true)).when(persistenceManager).pick(sql, sensorName);

        try {
            jpaStorage.getSensorWithTransaction(sensorName);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).pick(sql, sensorName);
        verify(persistenceManager, times(1)).rollback();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreSensorWithTransaction() {
        final Sensor sensor = new SensorBuilder().name("a_sensor").observationType(InsituObservation.class).pattern(4000000000L).build();

        doThrow(new PersistenceException(null, null, null, true)).when(persistenceManager).persist(sensor);

        try {
            jpaStorage.storeWithTransaction(sensor);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).persist(sensor);
        verify(persistenceManager, times(1)).rollback();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreSensorWithTransaction_failure() {
        final Sensor sensor = new SensorBuilder().name("a_sensor").observationType(InsituObservation.class).pattern(4000000000L).build();

        jpaStorage.storeWithTransaction(sensor);

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).persist(sensor);
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);
    }

    @Test
    public void testStoreInsituObservation() {
        final InsituObservation insituObservation = new InsituObservation();

        jpaStorage.store(insituObservation);

        verify(persistenceManager, times(1)).persist(insituObservation);
        verifyNoMoreInteractions(persistenceManager);
    }

    @SuppressWarnings("deprecation")
    private DataFile createDataFile(String path) {
        final DataFile dataFile = new DataFile(path, new SensorBuilder().name("cloud").build());
        dataFile.setId(ID);
        return dataFile;
    }

    private DataFile createDataFile(int id) {
        final DataFile dataFile = new DataFile("something", new SensorBuilder().name("cloud").build());
        dataFile.setId(id);
        return dataFile;
    }
}
