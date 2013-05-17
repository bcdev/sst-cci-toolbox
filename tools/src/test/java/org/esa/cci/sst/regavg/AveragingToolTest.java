package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.file.ProductType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class AveragingToolTest {

    @Test
    public void testOutputName() throws Exception {
        AveragingTool averagingTool = new AveragingTool();
        averagingTool.setProductType(ProductType.CCI_L3U);
        String filename = averagingTool.getOutputFilename("20000101", "20101231", "Global", ProcessingLevel.L3U, "SSTskin", "PS", "DM");
        assertEquals("20000101-20101231-Global_average-ESACCI-L3U_GHRSST-SSTskin-PS-DM-v2.0-fv1.1.nc", filename);
    }

    @Test
    public void testValidTemporalResolutions() throws Exception {
        assertEquals("[daily, monthly, seasonal, annual]", AveragingTool.validTemporalResolutions());
    }
}
