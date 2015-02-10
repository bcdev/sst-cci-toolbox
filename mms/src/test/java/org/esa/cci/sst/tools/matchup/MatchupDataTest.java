package org.esa.cci.sst.tools.matchup;


import org.esa.cci.sst.data.Sensor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MatchupDataTest {

    private MatchupData matchupData;

    @Before
    public void setUp() {
        matchupData = new MatchupData();
    }

    @Test
    public void testConstruction() {
        final List<IO_RefObservation> referenceObservations = matchupData.getReo();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());

        final List<IO_Observation> relatedObservations = matchupData.getRlo();
        assertNotNull(relatedObservations);
        assertTrue(relatedObservations.isEmpty());

        final List<IO_Observation> insituObservations = matchupData.getIso();
        assertNotNull(insituObservations);
        assertTrue(insituObservations.isEmpty());

        final List<Sensor> sensors = matchupData.getSe();
        assertNotNull(sensors);
        assertTrue(sensors.isEmpty());

        final List<IO_Matchup> matchups = matchupData.getMu();
        assertNotNull(matchups);
        assertTrue(matchups.isEmpty());
    }

    @Test
    public void testAdd_referenceObservation() {
        List<IO_RefObservation> referenceObservations = matchupData.getReo();
        assertEquals(0, referenceObservations.size());

        final IO_RefObservation io_refObs = new IO_RefObservation();
        matchupData.add(io_refObs);

        referenceObservations = matchupData.getReo();
        assertEquals(1, referenceObservations.size());
    }

    @Test
    public void testAdd_relatedObservation() {
        List<IO_Observation> relatedObservations = matchupData.getRlo();
        assertEquals(0, relatedObservations.size());

        final IO_Observation io_oObs = new IO_Observation();
        matchupData.addRelated(io_oObs);

        relatedObservations = matchupData.getRlo();
        assertEquals(1, relatedObservations.size());
    }

    @Test
    public void testAdd_insituObservation() {
        List<IO_Observation> insituObservations = matchupData.getIso();
        assertEquals(0, insituObservations.size());

        final IO_Observation io_oObs = new IO_Observation();
        matchupData.addInsitu(io_oObs);

        insituObservations = matchupData.getIso();
        assertEquals(1, insituObservations.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAdd_sensor() {
        List<Sensor> sensors = matchupData.getSe();
        assertEquals(0, sensors.size());

        matchupData.add(new Sensor());

        sensors = matchupData.getSe();
        assertEquals(1, sensors.size());
    }

    @Test
    public void testAdd_matchup() {
        List<IO_Matchup> matchups = matchupData.getMu();
        assertEquals(0, matchups.size());

        matchupData.add(new IO_Matchup());

        matchups = matchupData.getMu();
        assertEquals(1, matchups.size());
    }
}
