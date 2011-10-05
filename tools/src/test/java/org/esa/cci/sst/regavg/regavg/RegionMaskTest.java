package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.RegionMask;
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

    private void set(char[] data, int x, int y, char c) {
        data[73 * y + x] = c;
    }
}
