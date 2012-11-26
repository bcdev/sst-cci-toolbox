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

import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 21.09.12 09:48
 */
public class RegriddingOutputFileWriterTest {

    private Dimension latDim;
    private Dimension lonDim;
    private Dimension timeDim;
    private Dimension bndsDim;
    private Dimension[] measurementsDims;
    private RegriddingOutputFileWriter writerWithTotalUncertainty;
    private RegriddingOutputFileWriter writer;
    private NetcdfFileWriteable netcdfFileWithTotalUncertainty;
    private NetcdfFileWriteable netcdfFile;

    @Before
    public void setUp() throws Exception {
        writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", false);
        netcdfFile = NetcdfFileWriteable.createNew(new File("./notOnDisk.nc").getPath());
        latDim = netcdfFile.addDimension("lat", 180);
        lonDim = netcdfFile.addDimension("lon", 360);
        timeDim = netcdfFile.addDimension("time", 1, true, false, false);
        bndsDim = netcdfFile.addDimension("bnds", 2);

        writerWithTotalUncertainty = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", true);
        netcdfFileWithTotalUncertainty = NetcdfFileWriteable.createNew(new File("./notOnDisk.nc").getPath());
        netcdfFileWithTotalUncertainty.addDimension("lat", 180);
        netcdfFileWithTotalUncertainty.addDimension("lon", 360);
        netcdfFileWithTotalUncertainty.addDimension("time", 1, true, false, false);
        netcdfFileWithTotalUncertainty.addDimension("bnds", 2);

        measurementsDims = new Dimension[]{timeDim, latDim, lonDim};

    }

    @Test
    public void testOutputFileName() throws Exception {
        String startOfPeriod = "20120202";
        String endOfPeriod = "20120228";
        String regionName = "GLOBAL";
        String sstType = "SST_" + SstDepth.depth_20 + "_regridded";
        String productString = "PS";
        String additionalSegregator = "DM";
        String toolVersion = "0.1";
        String fileFormatVersion = "1.1";
        RegriddingOutputFileWriter regriddingTool = new RegriddingOutputFileWriter(ProductType.valueOf("CCI_L3C"),
                "toolName", toolVersion, fileFormatVersion, false);

        //execution
        String filename = regriddingTool.getOutputFilename(startOfPeriod, endOfPeriod, regionName,
                ProcessingLevel.L3C, sstType, productString, additionalSegregator);

        assertEquals("20120202-20120228-GLOBAL-ESACCI-L3C_GHRSST-SST_depth_20_regridded-PS-DM-v0.1-fv1.1.nc", filename);
    }

    @Test
    public void testCreateVariables_withTotalUncertainty() throws Exception {
        Variable[] variables = writerWithTotalUncertainty.createVariables(
                SstDepth.depth_20, netcdfFileWithTotalUncertainty, latDim, lonDim, bndsDim, measurementsDims);
        assertEquals(3, variables.length);
        assertEquals("sst_depth_20", variables[0].getName());
        assertEquals("sst_depth_20_anomaly", variables[1].getName());
        assertEquals("total_uncertainty", variables[2].getName());
    }

    @Test
    public void testCreateVariables() throws Exception {
        Variable[] variables = writer.createVariables(
                SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        assertEquals(7, variables.length);
        assertEquals("sst_depth_20", variables[0].getName());
        assertEquals("sst_depth_20_anomaly", variables[1].getName());
        assertEquals("coverage_uncertainty", variables[2].getName());
        assertEquals("uncorrelated_uncertainty", variables[3].getName());
        assertEquals("large_scale_correlated_uncertainty", variables[4].getName());
        assertEquals("synoptically_correlated_uncertainty", variables[5].getName());
        assertEquals("adjustment_uncertainty", variables[6].getName());
    }

    @Test
    public void testCalculateTotalUncertaintyFromUncertainties() throws Exception {
        final Variable[] variables = writerWithTotalUncertainty.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.0012, //[2] coverage uncertainty
                0.002,  //[3] uncorrelated uncertaity
                0.001,  //[4] large scale correlatee uncertainty
                0.02,   //[5] synoptically correlated uncertainty
                0.0034  //[6] adjustment uncertainty
        };

        final int startIndex = 2;
        final double totalUncertainty = RegriddingOutputFileWriter.calculateTotalUncertaintyFromUncertainties(variables, res, startIndex);
        assertEquals(0.0204, totalUncertainty, 1e-4);
    }
}
