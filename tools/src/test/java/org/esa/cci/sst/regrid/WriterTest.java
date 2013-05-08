package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.util.ProductType;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class WriterTest {

    @Test
    public void testTargetFileName() throws Exception {
        final String startOfPeriod = "20120202";
        final String endOfPeriod = "20120228";
        final String regionName = "GLOBAL";
        final String sstType = "SST_" + SstDepth.depth_20 + "_regridded";
        final String productString = "PS";
        final String additionalSegregator = "DM";
        final String toolVersion = "0.1";
        final String fileFormatVersion = "1.1";
        final Writer writer = new Writer(ProductType.valueOf("CCI_L3C"), "toolName", toolVersion, fileFormatVersion, false, 0.0);

        //execution
        final String filename = writer.getTargetFileName(startOfPeriod, endOfPeriod, regionName,
                                                           ProcessingLevel.L3C, sstType, productString,
                                                           additionalSegregator);

        assertEquals("20120202-20120228-GLOBAL-ESACCI-L3C_GHRSST-SST_depth_20_regridded-PS-DM-v0.1-fv1.1.nc", filename);
    }

}
