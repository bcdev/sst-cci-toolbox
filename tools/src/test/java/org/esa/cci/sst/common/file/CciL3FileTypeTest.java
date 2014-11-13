package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.AggregationCell;
import org.esa.cci.sst.common.CellAggregationCell;
import org.esa.cci.sst.common.CellFactory;
import org.esa.cci.sst.common.SpatialAggregationCell;
import org.esa.cci.sst.common.Grid;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.awt.*;

import static java.lang.Math.sqrt;
import static org.junit.Assert.*;


/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 14:32
 */
public class CciL3FileTypeTest {

    private static final FileType FILE_TYPE = CciL3FileType.INSTANCE;

    @Test
    public void testCell5Aggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setQualityGrid(new ScalarGrid(sourceGridDef, 5));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 1.0));
        context.setLargeScaleUncertaintyGrid(new ScalarGrid(sourceGridDef, 2.0));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);

        final SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        //execution
        cell5.accumulate(context, new Rectangle(0, 0, 100, 100));

        final int expectedN = 100 * 100;
        assertEquals(expectedN, cell5.getSampleCount());

        final Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1.0e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1.0e-6);
        assertEquals(1.2 * (1.0 - Math.pow(expectedN / 77500.0, 0.5)), results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1.0e-6);
        assertEquals(sqrt((0.8 * 0.8 * 10000) / ((0.8 * 10000) * (0.8 * 10000))), results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1.0e-6);
        assertEquals((2.0 * 0.8 * 10000) / (0.8 * 10000), results[Aggregation.LARGE_SCALE_UNCERTAINTY].doubleValue(), 1.0e-6);
    }

    @Test
    public void testCell90Aggregation_fromL3UCell5() throws Exception {
        final AggregationContext context = new AggregationContext();
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 3.0, 2.5));

        final CellFactory<CellAggregationCell<AggregationCell>> cell90Factory = FILE_TYPE.getCellFactory90(context);
        final CellAggregationCell<AggregationCell> cell90 = cell90Factory.createCell(0, 0);

        final AggregationCell cell5 = createCell5();

        //execution
        cell90.accumulate(cell5, 1.0);

        assertEquals(1, cell90.getSampleCount());
        final Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1e-6);
        assertEquals(1.3421176967534105, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(0.01, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(2.0, results[Aggregation.LARGE_SCALE_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(Double.NaN, results[Aggregation.SYNOPTIC_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(Double.NaN, results[Aggregation.ADJUSTMENT_UNCERTAINTY].doubleValue(), 1e-6);
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertFalse("Hallo".matches(FILE_TYPE.getFilenameRegex()));
        assertFalse("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz".matches(FILE_TYPE.getFilenameRegex()));
        assertFalse("19950723120045-ESACCI-L2C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex())); //Processing level 'L2C' is wrong
        assertFalse("20100701000000-ESACCI-L3A_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex())); //Processing level 'L3A' is wrong
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-04.1-01.1.nc".matches(
                FILE_TYPE.getFilenameRegex())); //'04.1-01.1' should be 'v04.1-fv01.1'
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin--LT-v04.1-fv01.1.nc".matches(
                FILE_TYPE.getFilenameRegex()));  //miss 'SEVIRI_SST'

        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3U_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3C_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTfnd-ATSR1-LT-v04.1-fv01.1.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20121101000000-ESACCI-L3U_GHRSST-SSTsubskin-ATSR2-LT-v04.1-fv01.1.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-v04.1-fv01.1.nc".matches(
                FILE_TYPE.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-SEVIRI_SST-LT-v04.1-fv01.1.nc".matches(
                FILE_TYPE.getFilenameRegex()));
    }

    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        //execution
        final AggregationContext context = FILE_TYPE.readSourceGrids(l3UFile, SstDepth.skin, new AggregationContext());

        //sea_surface_temperature
        final Grid sstGrid = context.getSstGrid();
        assertEquals(2000, sstGrid.getSampleInt(0, 0));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(0, 0), 1e-8);
        assertEquals(1000, sstGrid.getSampleInt(1, 0));
        assertEquals(283.14999367296696, sstGrid.getSampleDouble(1, 0), 1e-8);

        //quality_level
        final Grid qualityGrid = context.getQualityGrid();
        assertEquals(-127, qualityGrid.getSampleInt(0, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(0, 0), 1e-8);
        assertEquals(-127, qualityGrid.getSampleInt(1, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(1, 0), 1e-8);

        //uncorrelated_uncertainty
        final Grid randomUncertaintyGrid = context.getRandomUncertaintyGrid();
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(1, 0)));

        //large_scale_correlated_uncertainty
        final Grid largeScaleUncertaintyGrid = context.getLargeScaleUncertaintyGrid();
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(largeScaleUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(largeScaleUncertaintyGrid.getSampleDouble(1, 0)));

        //synoptically_correlated_uncertainty
        final Grid synopticUncertaintyGrid = context.getSynopticUncertaintyGrid();
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(synopticUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(synopticUncertaintyGrid.getSampleDouble(1, 0)));

        //adjustment_uncertainty
        final Grid adjustmentUncertaintyGrid = context.getAdjustmentUncertaintyGrid();
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(adjustmentUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(adjustmentUncertaintyGrid.getSampleDouble(1, 0)));
    }

    private SpatialAggregationCell createCell5() {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setQualityGrid(new ScalarGrid(sourceGridDef, 5));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 1.0));
        context.setLargeScaleUncertaintyGrid(new ScalarGrid(sourceGridDef, 2.0));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);
        final SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);

        cell5.accumulate(context, new Rectangle(0, 0, 100, 100));

        assertEquals(10000, cell5.getSampleCount());
        final Number[] results = cell5.getResults();

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1.0e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1.0e-6);
        assertEquals(0.7689472751357401, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(0.01, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1.0e-6);
        assertEquals(2.0, results[Aggregation.LARGE_SCALE_UNCERTAINTY].doubleValue(), 1.0e-6);

        return cell5;
    }
}
