package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.LUT1;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class LUT1Test {
    @Test
    public void testRead() throws Exception {
        LUT1 lut1 = LUT1.read(new File("./src/main/conf/auxdata/coverage_uncertainty_parameters.nc"));
        assertNotNull(lut1.getMagnitudeGrid5());
        assertNotNull(lut1.getExponentGrid5());
    }
}
