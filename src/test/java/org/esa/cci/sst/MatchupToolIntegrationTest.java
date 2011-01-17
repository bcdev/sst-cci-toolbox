package org.esa.cci.sst;

import org.junit.Ignore;

/**
 * matchup query pattern to be used with psql (very draft):
 *   select o1.id, o2.id, o1.time, o2.time from mm_observation o1, mm_observation o2 where o1.id > 656636 and o1.datafile_id = 417051 and o2.datafile_id = 657751 and (o1.time,o1.time+'12:00:00') overlaps (o2.time-'1 month', o2.time+'12:00:00'-'1 month') and st_distance(o1.location,o2.location) < 10400000;
 * @author Martin Boettcher
 */
@Ignore
public class MatchupToolIntegrationTest {

    public static void main(String[] args) {
        try {
            MatchupToolIntegrationTest t = new MatchupToolIntegrationTest();
            t.testFindCoincidences();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testFindCoincidences() throws Exception {
        new MatchupTool().findCoincidences();
    }
}
