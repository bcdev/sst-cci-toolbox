package org.esa.cci.sst.regavg.auxiliary;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class LUT2Test {
    @Test
    public void testRead() throws Exception {
        LUT2 lut2 = LUT2.read(new File("config/auxdata/RegionalAverage_LUT2.txt"));
        for (int m = 0; m < 12; m++) {
            assertEquals(0.822100, lut2.getMagnitude90(m, 0, 0), 1e-10);
            assertEquals(0.780967, lut2.getMagnitude90(m, 1, 0), 1e-10);
            assertEquals(0.987261, lut2.getMagnitude90(m, 2, 0), 1e-10);
            assertEquals(0.763564, lut2.getMagnitude90(m, 3, 0), 1e-10);
            assertEquals(0.641940, lut2.getMagnitude90(m, 0, 1), 1e-10);
            assertEquals(0.718666, lut2.getMagnitude90(m, 1, 1), 1e-10);
            assertEquals(0.582275, lut2.getMagnitude90(m, 2, 1), 1e-10);
            assertEquals(0.552870, lut2.getMagnitude90(m, 3, 1), 1e-10);
        }
    }
}
