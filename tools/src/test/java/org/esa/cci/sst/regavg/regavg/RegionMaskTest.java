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
        assertEquals(false, mask.getSample(-0.01, -0.01));
        assertEquals(true, mask.getSample(0.0, 0.0));
        assertEquals(true, mask.getSample(1.0, -1.0));
        assertEquals(true, mask.getSample(2.0, -2.0));
        assertEquals(true, mask.getSample(3.0, -3.0));
        assertEquals(true, mask.getSample(4.0, -4.0));
        assertEquals(true, mask.getSample(4.99, -4.99));
        assertEquals(false, mask.getSample(5.0, -5.0));

        assertEquals(false, mask.getSample(0.0, 0.01));
        assertEquals(true, mask.getSample(0.0, 0.0));
        assertEquals(true, mask.getSample(0.0, -1.0));
        assertEquals(true, mask.getSample(0.0, -2.0));
        assertEquals(true, mask.getSample(0.0, -3.0));
        assertEquals(true, mask.getSample(0.0, -4.0));
        assertEquals(true, mask.getSample(0.0, -4.99));
        assertEquals(false, mask.getSample(0.0, -5.0));

        assertEquals(false, mask.getSample( -0.01, 0.0));
        assertEquals(true,  mask.getSample( 0.0, 0.0));
        assertEquals(true,  mask.getSample( 1.0, 0.0));
        assertEquals(true,  mask.getSample( 2.0, 0.0));
        assertEquals(true,  mask.getSample( 3.0, 0.0));
        assertEquals(true,  mask.getSample( 4.0, 0.0));
        assertEquals(true,  mask.getSample( 4.99, 0.0));
        assertEquals(false, mask.getSample( 5.0, 0.0));
    }

    private void set(char[] data, int x, int y, char c) {
        data[73 * y + x] = c;
    }
}
