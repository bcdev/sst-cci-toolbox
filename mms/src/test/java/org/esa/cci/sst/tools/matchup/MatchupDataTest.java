package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MatchupDataTest {

    private MatchupData matchupData;

    @Before
    public void setUp() {
        matchupData = new MatchupData();
    }

    @Test
    public void testConstruction() {
        final List<IO_RefObservation> referenceObservations = matchupData.getReferenceObservations();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());

        final List<IO_Observation> relatedObservations = matchupData.getRelatedObservations();
        assertNotNull(relatedObservations);
        assertTrue(relatedObservations.isEmpty());

        final List<IO_Observation> insituObservations = matchupData.getInsituObservations();
        assertNotNull(insituObservations);
        assertTrue(insituObservations.isEmpty());

        final List<Sensor> sensors = matchupData.getSensors();
        assertNotNull(sensors);
        assertTrue(sensors.isEmpty());
    }

    @Test
    public void testAdd_referenceObservation() {
        List<IO_RefObservation> referenceObservations = matchupData.getReferenceObservations();
        assertEquals(0, referenceObservations.size());

        final IO_RefObservation io_refObs = new IO_RefObservation();
        matchupData.add(io_refObs);

        referenceObservations = matchupData.getReferenceObservations();
        assertEquals(1, referenceObservations.size());
    }

    @Test
    public void testAdd_relatedObservation() {
        List<IO_Observation> relatedObservations = matchupData.getRelatedObservations();
        assertEquals(0, relatedObservations.size());

        final IO_Observation io_oObs = new IO_Observation();
        matchupData.addRelated(io_oObs);

        relatedObservations = matchupData.getRelatedObservations();
        assertEquals(1, relatedObservations.size());
    }

    @Test
    public void testAdd_insituObservation() {
        List<IO_Observation> insituObservations = matchupData.getInsituObservations();
        assertEquals(0, insituObservations.size());

        final IO_Observation io_oObs = new IO_Observation();
        matchupData.addInsitu(io_oObs);

        insituObservations = matchupData.getInsituObservations();
        assertEquals(1, insituObservations.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAdd_sensor() {
        List<Sensor> sensors = matchupData.getSensors();
        assertEquals(0, sensors.size());

        matchupData.add(new Sensor());

        sensors = matchupData.getSensors();
        assertEquals(1, sensors.size());
    }
}
