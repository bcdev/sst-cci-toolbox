package org.esa.cci.sst.tools.matchup;


import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MatchupDataTest {

    private MatchupData matchupData;

    @Before
    public void setUp() {
        matchupData = new MatchupData();
    }

    @Test
    public void testConstruction() {
        final List<IO_RefObs> referenceObservations = matchupData.getReferenceObservations();
        assertNotNull(referenceObservations);
        assertTrue(referenceObservations.isEmpty());
    }

    @Test
    public void testAdd_referenceObservation() {
        List<IO_RefObs> referenceObservations = matchupData.getReferenceObservations();
        assertEquals(0, referenceObservations.size());

        final IO_RefObs io_refObs = new IO_RefObs();
        matchupData.add(io_refObs);

        referenceObservations = matchupData.getReferenceObservations();
        assertEquals(1, referenceObservations.size());
    }
}
