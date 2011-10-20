package org.esa.cci.sst.regavg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class RegionMaskTest {

    @Test
    public void testCenterFieldSet() throws Exception {
        char[] data = new char[73 * 36];
        for (int i = 0; i < data.length; i++) {
            if ((i + 1) % 73 == 0) {
                data[i] = '\n';
            } else {
                data[i] = '0';
            }
        }

        set(data, 36, 18, '1');

        RegionMask mask = RegionMask.create("test", new String(data));

        assertEquals(false, mask.getSampleBoolean(35, 17));
        assertEquals(false, mask.getSampleBoolean(36, 17));
        assertEquals(false, mask.getSampleBoolean(37, 17));

        assertEquals(false, mask.getSampleBoolean(35, 18));
        assertEquals(true, mask.getSampleBoolean(36, 18));
        assertEquals(false, mask.getSampleBoolean(37, 18));

        assertEquals(false, mask.getSampleBoolean(35, 19));
        assertEquals(false, mask.getSampleBoolean(36, 19));
        assertEquals(false, mask.getSampleBoolean(37, 19));
    }

    @Test
    public void testCoverage() throws Exception {
        assertEquals(RegionMask.Coverage.Globe, RegionMask.create("X", -180, +90, +180, -90).getCoverage());
        assertEquals(RegionMask.Coverage.N_Hemisphere, RegionMask.create("X", -180, +90, +180, 0).getCoverage());
        assertEquals(RegionMask.Coverage.S_Hemisphere, RegionMask.create("X", -180, 0, +180, -90).getCoverage());
        assertEquals(RegionMask.Coverage.Other, RegionMask.create("X", 0, 10, 10, 0).getCoverage());
    }

    private void set(char[] data, int x, int y, char c) {
        data[73 * y + x] = c;
    }
}
