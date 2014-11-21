package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.Matchup;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MatchupIOTest_mapping {

    @Test
    public void testMap_emptyList() {
        final IdGenerator idGenerator = new IdGenerator(2010, 11, 16, 17);
        final List<Matchup> matchups = new ArrayList<>();

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator);
        assertNotNull(mapped);
        assertTrue(mapped.getInsituObservations().isEmpty());
        assertTrue(mapped.getMatchups().isEmpty());
        assertTrue(mapped.getReferenceObservations().isEmpty());
        assertTrue(mapped.getRelatedObservations().isEmpty());
        assertTrue(mapped.getSensors().isEmpty());
    }

    @Test
    public void testRestore_emptyObject() {
        final MatchupData matchupData = new MatchupData();

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertTrue(matchups.isEmpty());
    }
}
