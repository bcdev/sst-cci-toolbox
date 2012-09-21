package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.util.ProductType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 21.09.12 09:48
 */
public class RegriddingToolTest {

    @Test
    public void testOutputFileName() throws Exception {
        String startOfPeriod = "20120202";
        String endOfPeriod = "20120228";
        String regionName = "GLOBAL";
        String sstType = "SST_" + SstDepth.depth_20 + "_regridded";
        String productString = "PS";
        String additionalSegregator = "DM";
        RegriddingTool regriddingTool = new RegriddingTool();
        regriddingTool.setProductType(ProductType.valueOf("CCI_L3C"));

        //execution
        String filename = regriddingTool.getOutputFilename(startOfPeriod, endOfPeriod, regionName,
                ProcessingLevel.L3C, sstType, productString, additionalSegregator);

        assertEquals("20120202-20120228-GLOBAL-ESACCI-L3C_GHRSST-SST_depth_20_regridded-PS-DM-v0.1-fv1.1.nc", filename);
    }
}
