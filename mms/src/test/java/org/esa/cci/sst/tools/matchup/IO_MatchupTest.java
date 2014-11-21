package org.esa.cci.sst.tools.matchup;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IO_MatchupTest {

    private IO_Matchup matchup;

    @Before
    public void setUp() {
        matchup = new IO_Matchup();
    }

    @Test
    public void testConstruction() {
        final List<IO_Coincidence> coincidences = matchup.getCoincidences();
        assertNotNull(coincidences);
        assertTrue(coincidences.isEmpty());
    }

    @Test
    public void testAddCoincidence() {
        List<IO_Coincidence> coincidences = matchup.getCoincidences();
        assertEquals(0, coincidences.size());

        matchup.add(new IO_Coincidence());

        coincidences = matchup.getCoincidences();
        assertEquals(1, coincidences.size());
    }
}
