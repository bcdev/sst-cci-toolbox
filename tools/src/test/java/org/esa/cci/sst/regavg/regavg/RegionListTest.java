package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.RegionMask;
import org.esa.cci.sst.regavg.RegionMaskList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class RegionListTest {

    @Test
    public void testGlobal() throws Exception {
        RegionMaskList regionMaskList = RegionMaskList.parse("Global=-180,90,180,-90");
        assertEquals(1, regionMaskList.size());
        RegionMask global = regionMaskList.get(0);
        assertEquals("Global", global.getName());
        for (int j = 0; j < 36; j++) {
            for (int i = 0; i < 72; i++) {
                assertEquals(true, global.getSampleBoolean(i, j));
            }
        }
    }

    @Test
    public void testTwo() throws Exception {
        RegionMaskList regionMaskList = RegionMaskList.parse("NW=-180,90,0,0;SE=0,0,180,-90");
        assertEquals(2, regionMaskList.size());

        RegionMask nw = regionMaskList.get(0);
        assertEquals("NW", nw.getName());
        for (int j = 0; j < 36; j++) {
            for (int i = 0; i < 72; i++) {
                assertEquals(String.format("i=%d,j=%d", i, j), i < 36 && j < 18, nw.getSampleBoolean(i, j));
            }
        }

        RegionMask se = regionMaskList.get(1);
        assertEquals("SE", se.getName());
        for (int j = 0; j < 36; j++) {
            for (int i = 0; i < 72; i++) {
                assertEquals(String.format("i=%d,j=%d", i, j), i >= 36 && j >= 18, se.getSampleBoolean(i, j));
            }
        }
    }

    @Test
    public void testAntiMerdian() throws Exception {
        RegionMaskList regionMaskList = RegionMaskList.parse("Pacific=170,10,-170,-10");
        assertEquals(1, regionMaskList.size());
        RegionMask pacific = regionMaskList.get(0);
        assertEquals("Pacific", pacific.getName());
        for (int j = 0; j < 36; j++) {
            for (int i = 0; i < 72; i++) {
                boolean iOk1 = i >= 0 && i <= 1;
                boolean iOk2 = i >= 70 && i <= 71;
                boolean jOk = j >= 16 && j <= 19;
                boolean expected = (iOk1  || iOk2) && jOk;
                assertEquals(String.format("i=%d,j=%d", i, j), expected, pacific.getSampleBoolean(i, j));
            }
        }
    }
}
