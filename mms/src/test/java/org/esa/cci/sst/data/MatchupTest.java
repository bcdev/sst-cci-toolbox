package org.esa.cci.sst.data;


import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MatchupTest {

    @Test
    public void testConstructor() {
        final Matchup matchup = new Matchup();

        final List<Coincidence> coincidences = matchup.getCoincidences();
        assertNotNull(coincidences);
        assertTrue(coincidences.isEmpty());
    }
}
