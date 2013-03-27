package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.ScalarGrid;
import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.SstDepth;
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

import java.awt.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 14:32
 */
public class CciL3FileTypeTest {
    FileType fileType = CciL3FileType.INSTANCE;

    @Test
    public void testGetFactory() throws Exception {
        assertTrue(fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1).createCell(0, 0) instanceof SpatialAggregationCell);
        assertTrue(fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_5).createCell(0, 0) instanceof CellAggregationCell);
        assertTrue(fileType.getCellFactory(FileType.CellTypes.SPATIAL_CELL_5).createCell(0, 0) instanceof SpatialAggregationCell);
        assertTrue(fileType.getCellFactory(FileType.CellTypes.SPATIAL_CELL_REGRIDDING).createCell(0, 0) instanceof SpatialAggregationCell);
        assertTrue(fileType.getCellFactory(FileType.CellTypes.CELL_90).createCell(0, 0) instanceof CellAggregationCell);
        assertTrue(fileType.getCellFactory(FileType.CellTypes.TEMPORAL_CELL).createCell(0, 0) instanceof CellAggregationCell);
    }

    @Test
    public void testSameMonthAggregation_testTypes() throws Exception {
        SameMonthAggregation aggregation = fileType.getSameMonthAggregationFactory().createAggregation();

        //L3UCell5
        SpatialAggregationCell filledL3UCell5 = createFilledL3UCell5();
        aggregation.accumulate(filledL3UCell5, 1.0);
        //SynopticCell5
        CellAggregationCell filledSynopticCell5 = createFilledSynopticCell5();
        aggregation.accumulate(filledSynopticCell5, 1.0);
        //L3UCell90
        CellAggregationCell filledL3UCell90 = createFilledL3UCell90();
        aggregation.accumulate(filledL3UCell90, 1.0);
    }

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
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(new MockCoverageUncertainty(1.1, 1.2, 0.5));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory(cellTypes);

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));

        assertEquals("zero samples counted", 0, cell5.getSampleCount());
        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 5, results.length);
        assertEquals(Double.NaN, results[0].doubleValue()); //sst
        assertEquals(Double.NaN, results[1].doubleValue()); //sstAnomaly
        assertEquals(Double.NaN, results[2].doubleValue()); //uncorrelatedUncertainty
        assertEquals(Double.NaN, results[3].doubleValue()); //largeScaleCorrelatedUncertainty
        assertEquals(Double.NaN, results[4].doubleValue()); //synopticallyCorrelatedUncertainty
//        assertEquals(Double.NaN, results[5].doubleValue()); //adjustmentUncertainty
//        assertEquals(Double.NaN, results[6].doubleValue()); //coverageUncertainty
    }


    @Test
    public void testL3USynopticCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        //ignored grids in L3USynopticAreaCell1
                        new ScalarGrid(sourceGridDef, 292.0), //[0] sstGrid
                        new ScalarGrid(sourceGridDef, 5), //[1] qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //[2] uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //[3] largeScaleCorrelatedUncertaintyGrid
                        //
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        CellFactory<SpatialAggregationCell> cellFactorySynoptic1 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1);
        SpatialAggregationCell cellSynoptic1 = cellFactorySynoptic1.createCell(0, 0);
        cellSynoptic1.accumulate(context, new Rectangle(0, 0, 10, 10));

        CellFactory<CellAggregationCell> cellFactorySynoptic5 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_5);
        CellAggregationCell cellSynoptic5 = cellFactorySynoptic5.createCell(0, 0);

        //execution
        cellSynoptic5.accumulate(cellSynoptic1, 0.5);
        cellSynoptic5.accumulate(cellSynoptic1, 0.2);
        cellSynoptic5.accumulate(cellSynoptic1, 0.8);
        cellSynoptic5.accumulate(cellSynoptic1, 0.8);

        assertEquals(4, cellSynoptic5.getSampleCount());
        Number[] results = cellSynoptic5.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 2, results.length);
        //todo could written here in a clearer way
        assertEquals(1.634343147277832, results[0].doubleValue(), 1e-6); //synopticallyCorrelatedUncertaintyGrid
        assertEquals(2.179124116897583, results[1].doubleValue(), 1e-6); //adjustmentUncertaintyGrid
    }

    @Test
    public void testL3USynopticAreaCell1Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        //ignored grids in L3USynopticAreaCell1
                        new ScalarGrid(sourceGridDef, 292.0), //[0] sstGrid
                        new ScalarGrid(sourceGridDef, 5), //[1] qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //[2] uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //[3] largeScaleCorrelatedUncertaintyGrid
                        //
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        CellFactory<SpatialAggregationCell> cellFactory = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1);
        SpatialAggregationCell cell = cellFactory.createCell(0, 0);

        //execution
        cell.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell.accumulate(context, new Rectangle(10, 0, 10, 10));

        int expectedN = 2 * 10 * 10;
        assertEquals(expectedN, cell.getSampleCount());
        Number[] results = cell.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 2, results.length);
        assertEquals(3.0, results[0].doubleValue(), 1e-6); //synopticallyCorrelatedUncertaintyGrid
        assertEquals(4.0, results[1].doubleValue(), 1e-6); //adjustmentUncertaintyGrid
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
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(new MockCoverageUncertainty(1.1, 1.2, 0.5));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory(cellTypes);

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
        assertEquals(sqrt((0.8 * 0.8 * 300) / ((0.8 * 300) * (0.8 * 300))), results[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals((2 * 0.8 * 300) / (0.8 * 300), results[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
    }

    @Test
    public void testCell90Aggregation_fromSynopticCell5() throws Exception {
        //preparation: 1) sources
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        //ignored grids in L3USynopticAreaCell1
                        new ScalarGrid(sourceGridDef, 292.0), //[0] sstGrid
                        new ScalarGrid(sourceGridDef, 5), //[1] qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //[2] uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //[3] largeScaleCorrelatedUncertaintyGrid
                        //
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        //preparation: 3) Synoptic1 -> Synoptic5
        CellFactory<SpatialAggregationCell> cellFactorySynoptic1 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1);
        SpatialAggregationCell cellSynoptic1 = cellFactorySynoptic1.createCell(0, 0);
        cellSynoptic1.accumulate(context, new Rectangle(0, 0, 10, 10));

        CellFactory<CellAggregationCell> cellFactorySynoptic5 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_5);
        CellAggregationCell cellSynoptic5 = cellFactorySynoptic5.createCell(0, 0);
        cellSynoptic5.accumulate(cellSynoptic1, 0.5);

        MockCoverageUncertainty provider = new MockCoverageUncertainty(1.1, 3.0, 2.5);
        FileType.CellTypes cellType = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(provider);
        CellFactory<CellAggregationCell> cell90Factory = fileType.getCellFactory(cellType);
        CellAggregationCell cell90 = cell90Factory.createCell(0, 0);

        //execution
        cell90.accumulate(cellSynoptic5, 1.0);

        assertEquals(1, cell90.getSampleCount());
        Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals("Expected count of accumulators", 7, results.length);
        assertEquals(Double.NaN, results[0].doubleValue(), 1e-6); //sst
        assertEquals(Double.NaN, results[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(Double.NaN, results[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(Double.NaN, results[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals(Double.NaN, results[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
        assertEquals(3.0, results[5].doubleValue(), 1e-6); //synopticallyCorrelatedUncertainty
        assertEquals(4.0, results[6].doubleValue(), 1e-6); //adjustmentUncertainty
    }

    @Test
    public void testCell90Aggregation_fromL3UCell5() throws Exception {
        MockCoverageUncertainty provider = new MockCoverageUncertainty(1.1, 3.0, 2.5);
        FileType.CellTypes cellType = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(provider);
        CellFactory<CellAggregationCell> cell90Factory = fileType.getCellFactory(cellType);
        CellAggregationCell cell90 = cell90Factory.createCell(0, 0);

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
    public void testCell90Aggregation_IllegalCell() throws Exception {
        MockCoverageUncertainty provider = new MockCoverageUncertainty(1.1, 3.0, 2.5);
        FileType.CellTypes cellType = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(provider);
        CellFactory<CellAggregationCell> cell90Factory = fileType.getCellFactory(cellType);
        CellAggregationCell cell90 = cell90Factory.createCell(0, 0);
        SpatialAggregationCell testCell = createTestSpatialAggregationCell();

        try {
            //execution
            cell90.accumulate(testCell, 0.5);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
            assertEquals("L3UCell5 or L3USynopticCell5 expected.", expected.getMessage());
        }
    }

    @Test
    public void testCell5Factory() throws Exception {
        MockCoverageUncertainty provider = new MockCoverageUncertainty(1.1, 3.0, 2.5);
        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(provider);
        CellFactory<SpatialAggregationCell> spatialAggregationCellFactory = fileType.getCellFactory(cellTypes);

        assertNotNull(spatialAggregationCellFactory);
        AggregationCell spatialAggregationCell = spatialAggregationCellFactory.createCell(18, 78);
        assertNotNull(spatialAggregationCell);
        assertEquals(18, spatialAggregationCell.getX());
        assertEquals(78, spatialAggregationCell.getY());
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertFalse("Hallo".matches(fileType.getFilenameRegex()));
        assertFalse("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz".matches(fileType.getFilenameRegex()));
        assertFalse("19950723120045-ESACCI-L2C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex())); //Processing level 'L2C' is wrong
        assertFalse("20100701000000-ESACCI-L3A_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex())); //Processing level 'L3A' is wrong
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-04.1-01.1.nc".matches(fileType.getFilenameRegex())); //'04.1-01.1' should be 'v04.1-fv01.1'
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin--LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));  //miss 'SEVIRI_SST'

        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3C_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3U_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L3C_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTfnd-ATSR1-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20121101000000-ESACCI-L3U_GHRSST-SSTsubskin-ATSR2-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-SEVIRI_SST-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));

    }

    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        //execution
        Grid[] grids = fileType.readSourceGrids(l3UFile, SstDepth.skin);
        //verification
        assertEquals(6, grids.length);

        //sea_surface_temperature
        assertEquals(2000, grids[0].getSampleInt(0, 0));
        assertEquals(293.14999344944954, grids[0].getSampleDouble(0, 0));
        assertEquals(1000, grids[0].getSampleInt(1, 0));
        assertEquals(283.14999367296696, grids[0].getSampleDouble(1, 0));
        //quality_level
        assertEquals(-127, grids[1].getSampleInt(0, 0));
        assertEquals(-127.0, grids[1].getSampleDouble(0, 0));
        assertEquals(-127, grids[1].getSampleInt(1, 0));
        assertEquals(-127.0, grids[1].getSampleDouble(1, 0));
        //uncorrelated_uncertainty
        assertEquals(-32768, grids[2].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(0, 0));
        assertEquals(-32768, grids[2].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(1, 0));
        //large_scale_correlated_uncertainty
        assertEquals(-32768, grids[3].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[3].getSampleDouble(0, 0));
        assertEquals(-32768, grids[3].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[3].getSampleDouble(1, 0));
        //synoptically_correlated_uncertainty
        assertEquals(-32768, grids[4].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[4].getSampleDouble(0, 0));
        assertEquals(-32768, grids[4].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[4].getSampleDouble(1, 0));
        //adjustment_uncertainty
        //Test file erroneous. Fill value is 'null'; actually it is in Byte and data in file are -127 in each cell
        assertEquals(-32768, grids[5].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[5].getSampleDouble(0, 0));
        assertEquals(-32768, grids[5].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[5].getSampleDouble(1, 0));
    }

    private SpatialAggregationCell createTestSpatialAggregationCell() {
        return new SpatialAggregationCell() {

            @Override
            public void accumulate(SpatialAggregationContext spatialAggregationContext, Rectangle rect) {
            }

            @Override
            public long getSampleCount() {
                return 0;
            }

            @Override
            public Number[] getResults() {
                return new Number[0];
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                return 0;
            }
        };
    }

    private CellAggregationCell createFilledL3UCell90() {
        SpatialAggregationCell filledL3UCell5 = createFilledL3UCell5();
        CellAggregationCell filledSynopticCell5 = createFilledSynopticCell5();

        MockCoverageUncertainty provider = new MockCoverageUncertainty(1.1, 3.0, 2.5);
        FileType.CellTypes cellType = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(provider);
        CellFactory<CellAggregationCell> cell90Factory = fileType.getCellFactory(cellType);
        CellAggregationCell cell90 = cell90Factory.createCell(0, 0);

        cell90.accumulate(filledL3UCell5, 1.0);
        cell90.accumulate(filledSynopticCell5, 1.0);

        assertEquals(1, cell90.getSampleCount());
        assertEquals(7, cell90.getResults().length);
        assertEquals(292.0, cell90.getResults()[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, cell90.getResults()[1].doubleValue(), 1e-6); //sstAnomaly
        assertEquals(1.5736546516418457, cell90.getResults()[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(0.05773502588272095, cell90.getResults()[3].doubleValue(), 1e-6); //uncorrelatedUncertainty
        assertEquals(2.0, cell90.getResults()[4].doubleValue(), 1e-6); //largeScaleCorrelatedUncertainty
        assertEquals(2.1213202476501465, cell90.getResults()[5].doubleValue(), 1e-6); //synopticallyCorrelatedUncertaintyGrid
        assertEquals(2.8284270763397217, cell90.getResults()[6].doubleValue(), 1e-6); //adjustmentUncertaintyGrid
        return cell90;
    }

    private CellAggregationCell createFilledSynopticCell5() {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        //ignored grids in L3USynopticAreaCell1
                        new ScalarGrid(sourceGridDef, 292.0), //sstGrid
                        new ScalarGrid(sourceGridDef, 5), //qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //largeScaleCorrelatedUncertaintyGrid
                        //
                        new ScalarGrid(sourceGridDef, 3.0), //synopticallyCorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 4.0), //adjustmentUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        CellFactory<SpatialAggregationCell> cellFactorySynoptic1 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1);
        SpatialAggregationCell cellSynoptic1 = cellFactorySynoptic1.createCell(0, 0);
        cellSynoptic1.accumulate(context, new Rectangle(0, 0, 10, 10));

        CellFactory<CellAggregationCell> cellFactorySynoptic5 = fileType.getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_5);
        CellAggregationCell cellSynoptic5 = cellFactorySynoptic5.createCell(0, 0);

        cellSynoptic5.accumulate(cellSynoptic1, 1.0);
        cellSynoptic5.accumulate(cellSynoptic1, 1.0);

        assertEquals(2, cellSynoptic5.getSampleCount());
        assertEquals(2.1213202476501465, cellSynoptic5.getResults()[0].doubleValue(), 1e-6); //synopticallyCorrelatedUncertaintyGrid
        assertEquals(2.8284270763397217, cellSynoptic5.getResults()[1].doubleValue(), 1e-6); //adjustmentUncertaintyGrid
        return cellSynoptic5;
    }

    private SpatialAggregationCell createFilledL3UCell5() {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //sstGrid
                        new ScalarGrid(sourceGridDef, 5), //qualityLevelGrid
                        new ScalarGrid(sourceGridDef, 1.0), //uncorrelatedUncertaintyGrid
                        new ScalarGrid(sourceGridDef, 2.0), //largeScaleCorrelatedUncertaintyGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        MockCoverageUncertainty uncertaintyProvider = new MockCoverageUncertainty(1.1, 1.2, 0.5);
        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(uncertaintyProvider);
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory(cellTypes);

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
