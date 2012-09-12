package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.ScalarGrid;
import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.ScalarCoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.esa.cci.sst.util.UTC;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static junit.framework.Assert.*;


/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 14:32
 */
public class CciL3UFileTypeTest {
    FileType fileType = CciL3UFileType.INSTANCE;

    @Test
    public void testCell5Aggregation_invalidQualityLevels() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //[0] sstGrid
                        new ScalarGrid(sourceGridDef, 2), //[1] qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //[2] uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //[3] largeScaleCorrelatedUncertaintyGrid
                        //ignored grids in L3UCell5
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8)); //seaCoverageGrid
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(
                new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));

        assertEquals("zero samples counted", 0, cell5.getSampleCount());
        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals("expected count of accumulators (result variables)", 5, results.length);
        assertEquals(Double.NaN, results[0].doubleValue()); //sst
        assertEquals(Double.NaN, results[1].doubleValue()); //sstAnomaly
        assertEquals(Double.NaN, results[2].doubleValue()); //uncorrelatedUncertainty
        assertEquals(Double.NaN, results[3].doubleValue()); //largeScaleCorrelatedUncertainty
        assertEquals(Double.NaN, results[4].doubleValue()); //synopticallyCorrelatedUncertainty
//        assertEquals(Double.NaN, results[5].doubleValue()); //adjustmentUncertainty
//        assertEquals(Double.NaN, results[6].doubleValue()); //coverageUncertainty
    }

    @Test
    public void testL3UCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
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
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(
                new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        //execution
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(20, 0, 10, 10));

        int expectedN = 3 * 10 * 10;
        assertEquals(expectedN, cell5.getSampleCount());
        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals("count of accumulators expected", 5, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, results[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)), results[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(sqrt((0.8 * 0.8 * 300) / ((0.8 * 300) * (0.8 * 300))), results[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals((2 * 0.8 * 300) / (0.8 * 300), results[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
    }

    @Test
    public void testCell5Factory() throws Exception {
        ScalarCoverageUncertaintyProvider provider = new ScalarCoverageUncertaintyProvider(1.1, 3.0, 2.5);
        CellFactory<? extends AggregationCell> spatialAggregationCellFactory = fileType.getSpatialAggregationCellFactory(provider);
        assertNotNull(spatialAggregationCellFactory);
        AggregationCell spatialAggregationCell = spatialAggregationCellFactory.createCell(18, 78);
        assertNotNull(spatialAggregationCell);
        assertEquals(18, spatialAggregationCell.getX());
        assertEquals(78, spatialAggregationCell.getY());
    }

    @Test
    public void testProcessingLevel() throws Exception {
        assertEquals(ProcessingLevel.L3U, fileType.getProcessingLevel());
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertFalse("Hallo".matches(fileType.getFilenameRegex()));
        assertFalse("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz".matches(fileType.getFilenameRegex()));
        assertFalse("19950723120045-ESACCI-L3C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-04.1-01.1.nc".matches(fileType.getFilenameRegex()));

        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3U_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTfnd-ATSR1-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20121101000000-ESACCI-L3U_GHRSST-SSTsubskin-ATSR2-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-SEVIRI_SST-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
    }

    @Test
    public void testParseDate() throws Exception {
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        assertEquals(format.parse("2010-07-01"), fileType.parseDate(new File("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));
        assertEquals(format.parse("2012-12-01"), fileType.parseDate(new File("20121201000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));
        assertEquals(format.parse("1995-07-31"), fileType.parseDate(new File("19950731000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));

        try {
            fileType.parseDate(new File("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz"));
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }

        try {
            fileType.parseDate(new File("A20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc"));
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }
    }

    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        //execution
        Grid[] grids = fileType.readSourceGrids(l3UFile, SstDepth.skin);
        //verification
        assertEquals(6, grids.length);

        assertEquals(2000, grids[0].getSampleInt(0, 0));
        assertEquals(293.14999344944954, grids[0].getSampleDouble(0, 0));
        assertEquals(1000, grids[0].getSampleInt(1, 0));
        assertEquals(283.14999367296696, grids[0].getSampleDouble(1, 0));

        assertEquals(-32768, grids[2].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(0, 0));
        assertEquals(-32768, grids[2].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(1, 0));

        assertEquals(-32768, grids[3].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[3].getSampleDouble(0, 0));
        assertEquals(-32768, grids[3].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[3].getSampleDouble(1, 0));

        assertEquals(-32768, grids[4].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[4].getSampleDouble(0, 0));
        assertEquals(-32768, grids[4].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[4].getSampleDouble(1, 0));

        //Test file erroneous. Fill value is 'null'; actually it is in Byte and data in file are -127 in each cell
        assertEquals(-32768, grids[5].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[5].getSampleDouble(0, 0));
        assertEquals(-32768, grids[5].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[5].getSampleDouble(1, 0));
    }

    public static double sqr(double x) {
        return x * x;
    }
}
