package org.esa.cci.sst.regavg.auxiliary;

import org.esa.cci.sst.common.cellgrid.Grid;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class LUT1Test {

    private static LUT1 lut1;

    @BeforeClass
    public static void read() throws IOException {
        File lutFile = new File("tools/config/auxdata/coverage_uncertainty_parameters.nc");
        if (!lutFile.isFile()) {
            lutFile = new File("config/auxdata/coverage_uncertainty_parameters.nc");
        }
        lut1 = LUT1.read(lutFile);
    }

    @Test
    public void testRead() {
        assertNotNull(lut1.getMagnitudeGrid5());
        assertNotNull(lut1.getExponentGrid5());
    }

    @Test
    public void testExponentGrid() {
        final Grid grid = lut1.getExponentGrid5();

        assertNotNull(grid);
        assertEquals(0.27232966f, (float) grid.getSampleDouble(0, 4), 0.0f);
        assertEquals(0.10357625f, (float) grid.getSampleDouble(39, 3), 0.0f);
    }

    @Test
    public void testMagnitudeGrid() {
        final Grid grid = lut1.getMagnitudeGrid5();

        assertNotNull(grid);
        assertEquals(0.9132683f, (float) grid.getSampleDouble(0, 4), 0.0f);
        assertEquals(0.7707501f, (float) grid.getSampleDouble(39, 3), 0.0f);
    }
}
