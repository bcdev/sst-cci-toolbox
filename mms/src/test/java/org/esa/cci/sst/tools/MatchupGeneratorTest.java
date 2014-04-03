package org.esa.cci.sst.tools;

import org.esa.cci.sst.common.InsituDatasetId;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MatchupGeneratorTest {

    @Test
    public void testCreateSensorShortName() {
        assertEquals("his_avhrr.n13", MatchupGenerator.createSensorShortName("history", "avhrr.n13"));
        assertEquals("ins_atsr.2", MatchupGenerator.createSensorShortName("insitu", "atsr.2"));
    }

    @Test
    public void testCreateSensorShortName_throwsOnNonStandardPrimarySensorname() {
        try {
            MatchupGenerator.createSensorShortName("history", "avhrr.full_resolution");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCreateReferenceObservation_sobolPoint() {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setIndex(77845);
        samplingPoint.setReferenceLon(22.2);
        samplingPoint.setReferenceLat(33.3);
        samplingPoint.setReferenceTime(776636483L);
        samplingPoint.setInsituDatasetId(InsituDatasetId.dummy_diurnal_variability);
        final DataFile datafile = new DataFile();

        final ReferenceObservation referenceObservation = MatchupGenerator.createReferenceObservation("Sobol", samplingPoint, datafile);
        assertNotNull(referenceObservation);
        assertEquals("77845", referenceObservation.getName());
        assertEquals("Sobol", referenceObservation.getSensor());

        final PGgeometry location = referenceObservation.getLocation();
        final PGgeometry point = referenceObservation.getPoint();
        assertSame(location, point);
        final Point locationPoint = location.getGeometry().getPoint(0);
        assertEquals(22.2, locationPoint.getX(), 1e-8);
        assertEquals(33.3, locationPoint.getY(), 1e-8);

        assertEquals(776636483L, referenceObservation.getTime().getTime());
        assertEquals(0.0, referenceObservation.getTimeRadius(), 1e-8);

        assertSame(datafile, referenceObservation.getDatafile());
        assertEquals(0, referenceObservation.getRecordNo());
        assertEquals(InsituDatasetId.dummy_diurnal_variability.getValue(), referenceObservation.getDataset());
        assertEquals(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED, referenceObservation.getReferenceFlag());
    }

    @Test
    public void testCreateReferenceObservation_insituPoint() {
        final SamplingPoint samplingPoint = new SamplingPoint(1, 2, 76636483L, Double.NaN);
        samplingPoint.setIndex(87845);
        samplingPoint.setReferenceLon(32.2);
        samplingPoint.setReferenceLat(43.3);
        samplingPoint.setReferenceTime(876636483L);
        samplingPoint.setInsituDatasetId(InsituDatasetId.radiometer);
        final DataFile datafile = new DataFile();

        final ReferenceObservation referenceObservation = MatchupGenerator.createReferenceObservation("Bottle", samplingPoint, datafile);
        assertNotNull(referenceObservation);
        assertEquals("87845", referenceObservation.getName());
        assertEquals("Bottle", referenceObservation.getSensor());

        final PGgeometry location = referenceObservation.getLocation();
        final PGgeometry point = referenceObservation.getPoint();
        assertSame(location, point);
        final Point locationPoint = location.getGeometry().getPoint(0);
        assertEquals(32.2, locationPoint.getX(), 1e-8);
        assertEquals(43.3, locationPoint.getY(), 1e-8);

        assertEquals(876636483L, referenceObservation.getTime().getTime());
        assertEquals(800000.0, referenceObservation.getTimeRadius(), 1e-8);

        assertSame(datafile, referenceObservation.getDatafile());
        assertEquals(0, referenceObservation.getRecordNo());
        assertEquals(InsituDatasetId.radiometer.getValue(), referenceObservation.getDataset());
        assertEquals(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED, referenceObservation.getReferenceFlag());
    }

    @Test
    public void testPersistReferenceObservations() {
        final Stack<EntityTransaction> transactionStack = new Stack<>();
        final EntityTransaction transaction = mock(EntityTransaction.class);
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);

        final ArrayList<ReferenceObservation> observations = new ArrayList<>();
        observations.add(new ReferenceObservation());
        observations.add(new ReferenceObservation());

        when(persistenceManager.transaction()).thenReturn(transaction);

        MatchupGenerator.persistReferenceObservations(observations, persistenceManager, transactionStack);

        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(2)).persist(any(ReferenceObservation.class));
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);

        assertEquals(1, transactionStack.size());
        assertSame(transaction, transactionStack.pop());
    }

    @Test
    public void testDefineMatchupPattern_twoSensors() {
        final String primarySensorname = "atsr.3";
        final String secondarySensorname = "atsr.2";
        final Stack<EntityTransaction> transactionStack = new Stack<>();
        final Sensor primarySensor = new SensorBuilder().name(primarySensorname).pattern(10).build();
        final Sensor secondarySensor = new SensorBuilder().name(secondarySensorname).pattern(100).build();

        final EntityTransaction transaction = mock(EntityTransaction.class);
        final Storage storage = mock(Storage.class);
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);

        when(persistenceManager.transaction()).thenReturn(transaction);
        when(persistenceManager.getStorage()).thenReturn(storage);

        when(storage.getSensor("orb_atsr.3")).thenReturn(primarySensor);
        when(storage.getSensor("orb_atsr.2")).thenReturn(secondarySensor);

        final long pattern = MatchupGenerator.defineMatchupPattern(primarySensorname, secondarySensorname, 1000000L, persistenceManager, transactionStack);

        assertEquals(1000000L | 10L | 100L, pattern);

        verify(persistenceManager, times(1)).getStorage();
        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);

        verify(storage, times(1)).getSensor("orb_atsr.3");
        verify(storage, times(1)).getSensor("orb_atsr.2");
        verifyNoMoreInteractions(storage);

        assertEquals(1, transactionStack.size());
        assertSame(transaction, transactionStack.pop());
    }

    @Test
    public void testDefineMatchupPattern_onlyPrimarySensor() {
        final String primarySensorname = "atsr.3";
        final Stack<EntityTransaction> transactionStack = new Stack<>();
        final Sensor primarySensor = new SensorBuilder().name(primarySensorname).pattern(10).build();

        final EntityTransaction transaction = mock(EntityTransaction.class);
        final Storage storage = mock(Storage.class);
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);

        when(persistenceManager.transaction()).thenReturn(transaction);
        when(persistenceManager.getStorage()).thenReturn(storage);

        when(storage.getSensor("orb_atsr.3")).thenReturn(primarySensor);

        final long pattern = MatchupGenerator.defineMatchupPattern(primarySensorname, null, 1000000L, persistenceManager, transactionStack);

        assertEquals(1000000L | 10L, pattern);

        verify(persistenceManager, times(1)).getStorage();
        verify(persistenceManager, times(1)).transaction();
        verify(persistenceManager, times(1)).commit();
        verifyNoMoreInteractions(persistenceManager);

        verify(storage, times(1)).getSensor("orb_atsr.3");
        verifyNoMoreInteractions(storage);

        assertEquals(1, transactionStack.size());
        assertSame(transaction, transactionStack.pop());
    }

    @Test
    public void testCreateInsituObservation() {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setDatasetName("ds_name");
        samplingPoint.setIndex(45);
        samplingPoint.setLon(33.89);
        samplingPoint.setLat(-11.88);
        samplingPoint.setTime(889834759837L);
        samplingPoint.setReferenceTime(889835759837L);
        final DataFile dataFile = new DataFile();

        final InsituObservation observation = MatchupGenerator.createInsituObservation(samplingPoint, dataFile);
        assertNotNull(observation);
        assertEquals(samplingPoint.getDatasetName(), observation.getName());
        assertSame(dataFile, observation.getDatafile());
        assertEquals(samplingPoint.getIndex(), observation.getRecordNo());
        assertEquals(Constants.SENSOR_NAME_HISTORY, observation.getSensor());

        final PGgeometry location = observation.getLocation();
        assertNotNull(location);
        final Geometry geometry = location.getGeometry();
        final Point firstPoint = geometry.getFirstPoint();
        assertEquals(samplingPoint.getLat(), firstPoint.getY(), 1e-8);
        assertEquals(samplingPoint.getLon(), firstPoint.getX(), 1e-8);

        assertEquals(samplingPoint.getTime(), observation.getTime().getTime());
        assertEquals(1000.0, observation.getTimeRadius(), 1e-8);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateMatchup() {
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setId(45);

        final Matchup matchup = MatchupGenerator.createMatchup(7765L, referenceObservation);
        assertNotNull(matchup);
        assertEquals(45, matchup.getId());
        assertSame(referenceObservation, matchup.getRefObs());
        assertEquals(7765L, matchup.getPattern());
    }

    @Test
    public void testCreatePrimaryCoincidence() {
        final Matchup matchup = new Matchup();
        final RelatedObservation relatedObservation = new RelatedObservation();

        final Coincidence coincidence = MatchupGenerator.createPrimaryCoincidence(matchup, relatedObservation);
        assertNotNull(coincidence);
        assertSame(matchup, coincidence.getMatchup());
        assertSame(relatedObservation, coincidence.getObservation());
        assertEquals(0.0, coincidence.getTimeDifference(), 1e-8);
    }

    @Test
    public void testCreateSecondaryCoincidence() {
        final Matchup matchup = new Matchup();
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setTime(new Date(1000000000L));
        matchup.setRefObs(referenceObservation);
        final RelatedObservation relatedObservation = new RelatedObservation();
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setReference2Time(1005000000L);

        final Coincidence coincidence = MatchupGenerator.createSecondaryCoincidence(samplingPoint, matchup, relatedObservation);
        assertNotNull(coincidence);
        assertSame(matchup, coincidence.getMatchup());
        assertSame(relatedObservation, coincidence.getObservation());
        assertEquals(5000, coincidence.getTimeDifference(), 1e-8);
    }
}
