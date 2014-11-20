package org.esa.cci.sst.tools.matchup;


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
}
