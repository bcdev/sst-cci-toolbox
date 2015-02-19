package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class MatchupComparatorTest {

    private MatchupComparator comparator;

    @Before
    public void setUp() {
        comparator = new MatchupComparator();
    }

    @Test
    public void testCompareByFilePath() {
        final Date now = new Date();
        final int anyId = 19;

        Matchup matchup_1 = createMatchup("/aaaa/a", now, anyId);
        Matchup matchup_2 = createMatchup("/bbb/b", now, anyId);
        assertEquals(-1, comparator.compare(matchup_1, matchup_2));

        matchup_1 = createMatchup("/cc/cc", now, anyId);
        matchup_2 = createMatchup("/bbb/b", now, anyId);
        assertEquals(1, comparator.compare(matchup_1, matchup_2));
    }

    @Test
    public void testCompareByFileTime() {
        final int anyId = 19;
        final String anyPath = "/aaaa/a";

        Matchup matchup_1 = createMatchup(anyPath, new Date(100000000L), anyId);
        Matchup matchup_2 = createMatchup(anyPath, new Date(200000000L), anyId);
        assertEquals(-1, comparator.compare(matchup_1, matchup_2));

        matchup_1 = createMatchup(anyPath, new Date(300000000L), anyId);
        matchup_2 = createMatchup(anyPath, new Date(200000000L), anyId);
        assertEquals(1, comparator.compare(matchup_1, matchup_2));
    }

    @Test
    public void testCompareById() {
        final Date now = new Date();
        final String anyPath = "/aaaa/a";

        Matchup matchup_1 = createMatchup(anyPath, now, 18);
        Matchup matchup_2 = createMatchup(anyPath, now, 19);
        assertEquals(-1, comparator.compare(matchup_1, matchup_2));

        matchup_1 = createMatchup(anyPath, now, 21);
        matchup_2 = createMatchup(anyPath, now, 20);
        assertEquals(1, comparator.compare(matchup_1, matchup_2));
    }

    @Test
    public void testCompare_same() {
        final Date now = new Date();
        final String anyPath = "/aaaa/a";
        final int id = 198;

        Matchup matchup_1 = createMatchup(anyPath, now, id);
        Matchup matchup_2 = createMatchup(anyPath, now, id);
        assertEquals(0, comparator.compare(matchup_1, matchup_2));
    }

    @SuppressWarnings("deprecation")
    private Matchup createMatchup(String path, Date refObsTime, int id) {
        final Matchup matchup = new Matchup();
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setTime(refObsTime);
        referenceObservation.setId(id);
        referenceObservation.setDatafile(new DataFile(path, null));
        matchup.setRefObs(referenceObservation);
        return matchup;
    }
}
