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
 * {@author Bettina Scholze}
 * Date: 21.09.12 09:48
 */
public class RegriddingOutputFileWriterTest {

    private Dimension latDim;
    private Dimension lonDim;
    private Dimension bndsDim;
    private Dimension[] measurementsDims;
    private NetcdfFileWriteable netcdfFileWithTotalUncertainty;
    private NetcdfFileWriteable netcdfFile;

    @Before
    public void setUp() throws Exception {
        netcdfFile = NetcdfFileWriteable.createNew(new File("./notOnDisk.nc").getPath());
        latDim = netcdfFile.addDimension("lat", 180);
        lonDim = netcdfFile.addDimension("lon", 360);
        Dimension timeDim = netcdfFile.addDimension("time", 1, true, false, false);
        bndsDim = netcdfFile.addDimension("bnds", 2);

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
                "toolName", toolVersion, fileFormatVersion, false, 0.0);

        //execution
        String filename = regriddingTool.getOutputFilename(startOfPeriod, endOfPeriod, regionName,
                ProcessingLevel.L3C, sstType, productString, additionalSegregator);

        assertEquals("20120202-20120228-GLOBAL-ESACCI-L3C_GHRSST-SST_depth_20_regridded-PS-DM-v0.1-fv1.1.nc", filename);
    }

    @Test
    public void testCreateVariables_withTotalUncertainty() throws Exception {
        RegriddingOutputFileWriter writerWithTotalUncertainty = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", true, 0.5);

        Variable[] variables = writerWithTotalUncertainty.createVariables(
                SstDepth.depth_20, netcdfFileWithTotalUncertainty, latDim, lonDim, bndsDim, measurementsDims);
        assertEquals(3, variables.length);
        assertEquals("sst_depth_20", variables[0].getName());
        assertEquals("sst_depth_20_anomaly", variables[1].getName());
        assertEquals("total_uncertainty", variables[2].getName());
    }

    @Test
    public void testCreateVariables() throws Exception {
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", false, 0.5);

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
    public void testCalculateTotalUncertaintyFromUncertainties_withNaN() throws Exception {
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "", "", "", false, 0.5);

        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);

        Number[] res = new Number[]{
                280.4,      //[0] sst
                -2.9,       //[1] sst anomaly
                0.0012,     //[2] coverage uncertainty
                0.002,      //[3] uncorrelated uncertainty
                0.002,      //[4] large scale correlatee uncertainty
                Double.NaN, //[5] synoptically correlated uncertainty
                0.0034      //[6] adjustment uncertainty
        };

        final double totalUncertainty = RegriddingOutputFileWriter.calculateTotalUncertainty(variables, res);
        assertEquals(0.0046, totalUncertainty, 1e-4);
    }

    @Test
    public void testCalculateTotalUncertaintyFromUncertainties() throws Exception {

        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "", "", "", false, 0.5);
        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.0012, //[2] coverage uncertainty
                0.002,  //[3] uncorrelated uncertainty
                0.001,  //[4] large scale correlatee uncertainty
                0.02,   //[5] synoptically correlated uncertainty
                0.0034  //[6] adjustment uncertainty
        };

        final double totalUncertainty = RegriddingOutputFileWriter.calculateTotalUncertainty(variables, res);
        assertEquals(0.0204, totalUncertainty, 1e-4);
    }

    @Test
    public void testFillInDataMap_withTotalUncertainty() throws Exception {

        RegriddingOutputFileWriter writerWithTotalUncertainty = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", true, 0.5);
        final Variable[] variables = writerWithTotalUncertainty.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writerWithTotalUncertainty.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.0012, //[2] coverage uncertainty
                0.002,  //[3] uncorrelated uncertainty
                0.001,  //[4] large scale correlatee uncertainty
                0.02,   //[5] synoptically correlated uncertainty
                0.0034  //[6] adjustment uncertainty
        };
        //execution
        writerWithTotalUncertainty.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(3, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(280.4, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(-2.9, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(0.02044, dataMap.get("total_uncertainty").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_withTotalUncertainty_totalUncertaintyMaximumExceeded() throws Exception {
        final double maxTotalUncertainty = 0.0;
        final boolean totalUncertaintyWanted = true;
        RegriddingOutputFileWriter writerWithTotalUncertainty = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writerWithTotalUncertainty.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writerWithTotalUncertainty.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.77, //[2] coverage uncertainty
                0.90,  //[3] uncorrelated uncertainty
                0.89,  //[4] large scale correlatee uncertainty
                0.59,   //[5] synoptically correlated uncertainty
                0.88    //[6] adjustment uncertainty
        };
        //execution
        writerWithTotalUncertainty.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(3, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(Double.NaN, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("total_uncertainty").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_totalUncertaintyMaximumExceeded() throws Exception {
        final double maxTotalUncertainty = 0.0;
        final boolean totalUncertaintyWanted = false;
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.77,   //[2] coverage uncertainty
                0.90,   //[3] uncorrelated uncertainty
                0.89,   //[4] large scale correlatee uncertainty
                0.59,   //[5] synoptically correlated uncertainty
                0.88    //[6] adjustment uncertainty
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(7, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(Double.NaN, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("uncorrelated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("large_scale_correlated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("synoptically_correlated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("adjustment_uncertainty").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap() throws Exception {

        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L3U, "myTool", "v0.0", "NetCDF", false, 0.5);
        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.0012, //[2] coverage uncertainty
                0.002,  //[3] uncorrelated uncertainty
                0.001,  //[4] large scale correlatee uncertainty
                0.02,   //[5] synoptically correlated uncertainty
                0.0034  //[6] adjustment uncertainty
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(7, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(280.4, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(-2.9, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(0.0012, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.002, dataMap.get("uncorrelated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.001, dataMap.get("large_scale_correlated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.02, dataMap.get("synoptically_correlated_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.0034, dataMap.get("adjustment_uncertainty").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_L4() throws Exception {

        final boolean totalUncertaintyWanted = false; //but is ignored for L4
        final double maxTotalUncertainty = 1.0;
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L4, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writer.createVariables(SstDepth.skin, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.65,   //[2] sea ice fraction
                0.0012, //[3] coverage uncertainty
                0.002   //[4] analysis error average
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(5, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_skin").vec.length);
        //verification
        assertEquals(280.4, dataMap.get("sst_skin").getAsFloats()[0], 1e-2);
        assertEquals(-2.9, dataMap.get("sst_skin_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(0.65, dataMap.get("sea_ice_fraction").getAsFloats()[0], 1e-4);
        assertEquals(0.0012, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.002, dataMap.get("analysis_error").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_L4_totalUncertaintyMaximumExceeded() throws Exception {

        final boolean totalUncertaintyWanted = false; //but is ignored for L4
        final double maxTotalUncertainty = 0.0;
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.CCI_L4, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writer.createVariables(SstDepth.skin, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.65,   //[2] sea ice fraction
                0.0012, //[3] coverage uncertainty
                0.002   //[4] analysis error average
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(5, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_skin").vec.length);
        //verification
        assertEquals(Double.NaN, dataMap.get("sst_skin").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("sst_skin_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("sea_ice_fraction").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("analysis_error").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_Arc() throws Exception {

        final boolean totalUncertaintyWanted = false; //but is ignored for Arc
        final double maxTotalUncertainty = 1.0;
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.ARC_L3U, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.011,  //[2] coverage uncertainty
                0.020,  //[3] arc uncertainty
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(4, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(280.4, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(-2.9, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(0.011, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(0.020, dataMap.get("arc_uncertainty").getAsFloats()[0], 1e-4);
    }

    @Test
    public void testFillInDataMap_Arc_totalUncertaintyMaximumExceeded() throws Exception {

        final boolean totalUncertaintyWanted = false; //but is ignored for Arc
        final double maxTotalUncertainty = 0.0;
        RegriddingOutputFileWriter writer = new RegriddingOutputFileWriter(ProductType.ARC_L3U, "", "", "", totalUncertaintyWanted, maxTotalUncertainty);
        final Variable[] variables = writer.createVariables(SstDepth.depth_20, netcdfFile, latDim, lonDim, bndsDim, measurementsDims);
        final HashMap<String, RegriddingOutputFileWriter.VectorContainer> dataMap =
                writer.initialiseDataMap(variables, latDim.getLength(), lonDim.getLength());

        Number[] res = new Number[]{
                280.4,  //[0] sst
                -2.9,   //[1] sst anomaly
                0.011,  //[2] coverage uncertainty
                0.020,  //[3] arc uncertainty
        };
        //execution
        writer.fillInDataMap(variables, dataMap, res, 0);
        assertEquals(4, dataMap.size());
        assertEquals(latDim.getLength() * lonDim.getLength(), dataMap.get("sst_depth_20").vec.length);
        //verification
        assertEquals(Double.NaN, dataMap.get("sst_depth_20").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("sst_depth_20_anomaly").getAsFloats()[0], 1e-2);
        assertEquals(Double.NaN, dataMap.get("coverage_uncertainty").getAsFloats()[0], 1e-4);
        assertEquals(Double.NaN, dataMap.get("arc_uncertainty").getAsFloats()[0], 1e-4);
    }
}
