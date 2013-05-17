package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.file.ProductType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class WriterTest {

    @Test
    public void testTargetFilename() throws Exception {
        final String startOfPeriod = "20120202";
        final String endOfPeriod = "20120228";
        final String regionName = "GLOBAL";
        final String sstType = "SST_" + SstDepth.depth_20;
        final String productString = "regridded0.5";
        final String additionalSegregator = "DM";
        final String toolVersion = "0.1";
        final String fileFormatVersion = "1.1";
        final Writer writer = new Writer(ProductType.valueOf("CCI_L3C"), "toolName", toolVersion, fileFormatVersion,
                                         false, 0.0, null, null, null, null, null);
        //execution
        final String filename = writer.getTargetFilename(startOfPeriod, endOfPeriod, regionName,
                                                         ProcessingLevel.L3C, sstType, productString,
                                                         additionalSegregator);

        assertEquals("20120202-20120228-GLOBAL-ESACCI-L3C_GHRSST-SST_depth_20-regridded0.5-DM-v0.1-fv1.1.nc", filename);
    }

}
