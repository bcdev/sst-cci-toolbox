package org.esa.cci.sst.tools.regavg.auxiliary;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;


/**
 * @author Norman Fomferra
 */
public class LUT2Test {
    @Test
    public void testRead() throws Exception {
        final URL resource = LUT1Test.class.getResource("RegionalAverage_LUT2.txt");
        final String lutFilePath = resource.getFile();

        final LUT2 lut2 = LUT2.read(new File(lutFilePath));

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
