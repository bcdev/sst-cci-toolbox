package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.RegionMask;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class RegionMaskTest {

    @Test
    public void testSettings() throws Exception {
        char[] data = new char[73 * 36];
        for (int i = 0; i < data.length; i++) {
            if ((i + 1) % 73 == 0) {
                data[i] = '\n';
            } else {
                data[i] = '0';
            }
        }

        set(data, 36, 18, '1');

        RegionMask eq5x5at00 = RegionMask.create("Eq5x5at00", new String(data));
        assertEquals(false, eq5x5at00.getSample(-0.01, -0.01));
        assertEquals(true, eq5x5at00.getSample(0, 0));
        assertEquals(true, eq5x5at00.getSample(1, 1));
        assertEquals(true, eq5x5at00.getSample(2, 2));
        assertEquals(true, eq5x5at00.getSample(3, 3));
        assertEquals(true, eq5x5at00.getSample(5, 4));
        assertEquals(true, eq5x5at00.getSample(4.99, 4.99));
        assertEquals(false, eq5x5at00.getSample(5, 5));
    }

    private void set(char[] data, int x, int y, char c) {
        data[72 * y + x] = c;
    }
}
