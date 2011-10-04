package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.RegionalAverageTool;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class RegionalAverageToolTest {

    @Test
    public void testOutputName() throws Exception {
        String filename = RegionalAverageTool.getOutputFilename("20000101", "20101231", "Global", ProcessingLevel.L3U, "SSTskin", "PS", "DM", "01.0");
        assertEquals("20000101-20101231-Global_average-ESACCI-L3U_GHRSST-SSTskin-PS-DM-v02.0-fv01.0.nc", filename);
    }

}
