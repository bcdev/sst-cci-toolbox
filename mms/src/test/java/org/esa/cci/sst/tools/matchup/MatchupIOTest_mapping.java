package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.Matchup;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MatchupIOTest_mapping {

    private IdGenerator idGenerator;

    @Before
    public void setUp() {
        idGenerator = new IdGenerator(2010, 11, 16, 17);
    }

    @Test
    public void testMap_emptyList() {
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
    public void testMap_oneMatchup_oneRefObs() {
        final List<Matchup> matchups = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setPattern(1L);
        matchup.setInvalid(true);
        matchups.add(matchup);

        final MatchupData mapped = MatchupIO.map(matchups, idGenerator);
        assertNotNull(mapped);
        assertTrue(mapped.getInsituObservations().isEmpty());
        assertTrue(mapped.getRelatedObservations().isEmpty());
        assertTrue(mapped.getSensors().isEmpty());

        final List<IO_Matchup> mappedMatchups = mapped.getMatchups();
        assertEquals(1, mappedMatchups.size());
        final IO_Matchup io_matchup = mappedMatchups.get(0);
        assertEquals(2010111617000000000L, io_matchup.getId());
        assertEquals(1, io_matchup.getPattern());
        assertTrue(io_matchup.isInvalid());

//        final List<IO_RefObservation> referenceObservations = mapped.getReferenceObservations();
//        assertEquals(1, referenceObservations.size());
    }

    @Test
    public void testRestore_emptyObject() {
        final MatchupData matchupData = new MatchupData();

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertTrue(matchups.isEmpty());
    }

    @Test
    public void testRestore_oneMatchup_oneRefObs() {
        final MatchupData matchupData = new MatchupData();
        final IO_Matchup io_matchup = new IO_Matchup();
        io_matchup.setId(2);
        io_matchup.setPattern(3);
        io_matchup.setInvalid(false);
        matchupData.add(io_matchup);

        final List<Matchup> matchups = MatchupIO.restore(matchupData);
        assertEquals(1, matchups.size());
        final Matchup matchup = matchups.get(0);
        assertEquals(2, matchup.getId());
        assertEquals(3, matchup.getPattern());
        assertFalse(matchup.isInvalid());
    }
}
