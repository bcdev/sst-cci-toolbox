package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.ScalarGrid;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.awt.Rectangle;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 14:32
 */
public class CciL3FileTypeTest {

    FileType fileType = CciL3FileType.INSTANCE;

    @Test
    public void testL3UCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        AggregationContext context = new AggregationContext(
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //sstGrid
                        new ScalarGrid(sourceGridDef, 5), //qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //largeScaleCorrelatedUncertaintyGrid
                        //ignored grids in L3UCell5
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8)); //seaCoverageGrid

        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory5(context);

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        //execution
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(20, 0, 10, 10));

        int expectedN = 3 * 10 * 10;
        assertEquals(expectedN, cell5.getSampleCount());
        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 5, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, results[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)), results[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(sqrt((0.8 * 0.8 * 300) / ((0.8 * 300) * (0.8 * 300))), results[3].doubleValue(),
                     1e-6); //uncorrelatedUncertainty
        assertEquals((2 * 0.8 * 300) / (0.8 * 300), results[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
    }

    @Test
    public void testCell90Aggregation_fromL3UCell5() throws Exception {
        final AggregationContext context = new AggregationContext();
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 3.0, 2.5));
        CellFactory<CellAggregationCell<AggregationCell>> cell90Factory = fileType.getCellFactory90(context);
        CellAggregationCell<AggregationCell> cell90 = cell90Factory.createCell(0, 0);

        SpatialAggregationCell filledL3UCell5 = createFilledL3UCell5();

        //execution
        cell90.accumulate(filledL3UCell5, 1.0);

        assertEquals(1, cell90.getSampleCount());
        Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 7, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, results[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(1.5736546516418457, results[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(0.05773502588272095, results[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals(2.0, results[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
        assertEquals(Double.NaN, results[5].doubleValue(), 1e-6); //synopticallyCorrelatedUncertainty
        assertEquals(Double.NaN, results[6].doubleValue(), 1e-6); //adjustmentUncertainty
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertFalse("Hallo".matches(fileType.getFilenameRegex()));
        assertFalse("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz".matches(fileType.getFilenameRegex()));
        assertFalse("19950723120045-ESACCI-L2C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex())); //Processing level 'L2C' is wrong
        assertFalse("20100701000000-ESACCI-L3A_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex())); //Processing level 'L3A' is wrong
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-04.1-01.1.nc".matches(
                fileType.getFilenameRegex())); //'04.1-01.1' should be 'v04.1-fv01.1'
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin--LT-v04.1-fv01.1.nc".matches(
                fileType.getFilenameRegex()));  //miss 'SEVIRI_SST'

        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3U_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3C_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTfnd-ATSR1-LT-v04.1-fv01.1.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20121101000000-ESACCI-L3U_GHRSST-SSTsubskin-ATSR2-LT-v04.1-fv01.1.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-v04.1-fv01.1.nc".matches(
                fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-SEVIRI_SST-LT-v04.1-fv01.1.nc".matches(
                fileType.getFilenameRegex()));
    }

    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        //execution
        final AggregationContext context = fileType.readSourceGrids(l3UFile, SstDepth.skin, new AggregationContext());

        //sea_surface_temperature
        final Grid sstGrid = context.getSstGrid();
        assertEquals(2000, sstGrid.getSampleInt(0, 0));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(0, 0));
        assertEquals(1000, sstGrid.getSampleInt(1, 0));
        assertEquals(283.14999367296696, sstGrid.getSampleDouble(1, 0));
        //quality_level
        final Grid qualityGrid = context.getQualityGrid();
        assertEquals(-127, qualityGrid.getSampleInt(0, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(0, 0));
        assertEquals(-127, qualityGrid.getSampleInt(1, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(1, 0));
        //uncorrelated_uncertainty
        final Grid randomUncertaintyGrid = context.getRandomUncertaintyGrid();
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(0, 0));
        assertEquals(Double.NaN, randomUncertaintyGrid.getSampleDouble(0, 0));
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(1, 0));
        assertEquals(Double.NaN, randomUncertaintyGrid.getSampleDouble(1, 0));
        //large_scale_correlated_uncertainty
        final Grid largeScaleUncertaintyGrid = context.getLargeScaleUncertaintyGrid();
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(0, 0));
        assertEquals(Double.NaN, largeScaleUncertaintyGrid.getSampleDouble(0, 0));
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(1, 0));
        assertEquals(Double.NaN, largeScaleUncertaintyGrid.getSampleDouble(1, 0));
        //synoptically_correlated_uncertainty
        final Grid synopticUncertaintyGrid = context.getSynopticUncertaintyGrid();
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(0, 0));
        assertEquals(Double.NaN, synopticUncertaintyGrid.getSampleDouble(0, 0));
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(1, 0));
        assertEquals(Double.NaN, synopticUncertaintyGrid.getSampleDouble(1, 0));
        //adjustment_uncertainty
        final Grid adjustmentUncertaintyGrid = context.getAdjustmentUncertaintyGrid();
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(0, 0));
        assertEquals(Double.NaN, adjustmentUncertaintyGrid.getSampleDouble(0, 0));
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(1, 0));
        assertEquals(Double.NaN, adjustmentUncertaintyGrid.getSampleDouble(1, 0));
    }

    private SpatialAggregationCell createFilledL3UCell5() {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        AggregationContext context = new AggregationContext(
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //sstGrid
                        new ScalarGrid(sourceGridDef, 5), //qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //largeScaleCorrelatedUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8)); //seaCoverageGrid

        final CoverageUncertaintyProvider uncertaintyProvider = new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5);
        context.setCoverageUncertaintyProvider(uncertaintyProvider);
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory5(context);

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(20, 0, 10, 10));

        assertEquals(300, cell5.getSampleCount());
        assertEquals(292.0, cell5.getResults()[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, cell5.getResults()[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(1.1253395080566406, cell5.getResults()[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(0.05773502588272095, cell5.getResults()[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals(2.0, cell5.getResults()[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty

        return cell5;
    }
}
