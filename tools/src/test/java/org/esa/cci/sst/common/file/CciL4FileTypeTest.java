package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.ScalarGrid;
import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.ScalarCoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.awt.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * {@author Bettina Scholze}
 * Date: 17.09.12 11:02
 */
public class CciL4FileTypeTest {
    FileType fileType = CciL4FileType.INSTANCE;

    @Test
    public void testL3UCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1); //whatever
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //sstGrid
                        new ScalarGrid(sourceGridDef, 1.0), //analysisErrorGrid
                        new ScalarGrid(sourceGridDef, 0.5), //seaIceFractionGrid
                },
                new ScalarGrid(sourceGridDef, 291.5), //analysedSstGrid
                new ScalarGrid(sourceGridDef, 0.8), null); //seaCoverageGrid

        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));
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
        assertEquals(sqrt((0.8 * 0.8 * 300) / ((0.8 * 300) * (0.8 * 300))), results[3].doubleValue(), 1e-6); //analysisError
        assertEquals((0.5 * 300) / 300, results[4].doubleValue(), 1e-6); //seaIceFraction
    }

    @Test
    public void testCell90Aggregation() throws Exception {
        ScalarCoverageUncertaintyProvider coverageUncertaintyProvider = new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5);
        FileType.CellTypes cellType = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(coverageUncertaintyProvider);
        CellFactory<CellAggregationCell> cell90Factory = fileType.getCellFactory(cellType);
        CellAggregationCell cell90 = cell90Factory.createCell(0, 0);

        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0), //analysed_sst
                        new ScalarGrid(sourceGridDef, 0.1), //analysis_error
                        new ScalarGrid(sourceGridDef, 0.5), //sea_ice_fraction
                },
                new ScalarGrid(sourceGridDef, 291.5),
                new ScalarGrid(sourceGridDef, 0.8), null);

        FileType.CellTypes cellTypes = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(coverageUncertaintyProvider);
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getCellFactory(cellTypes);

        SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_4.accumulate(context, new Rectangle(10, 10, 10, 10));

        int expectedN5_1 = 100;
        int expectedN5_2 = 100;
        int expectedN5_3 = 100;
        int expectedN5_4 = 200;
        assertEquals(expectedN5_1, cell5_1.getSampleCount());
        assertEquals(expectedN5_2, cell5_2.getSampleCount());
        assertEquals(expectedN5_3, cell5_3.getSampleCount());
        assertEquals(expectedN5_4, cell5_4.getSampleCount());

        cell90.accumulate(cell5_1, 0.25); // --> w=0.125, n = 100
        cell90.accumulate(cell5_2, 0.5);  // --> w=0.25, n = 100
        cell90.accumulate(cell5_3, 0.25); // --> w=0.125, n = 100
        cell90.accumulate(cell5_4, 1.0);  // --> w=0.5, n = 200

        int expectedN90 = 4;
        assertEquals(expectedN90, cell90.getSampleCount());

        Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals(5, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6); //sst
        assertEquals(0.5, results[1].doubleValue(), 1e-6); //sstAnomaly
        // todo - replace inexplicable numbers by formulas, testCell5Aggregation() (nf)
        assertEquals(0.86736869, results[2].doubleValue(), 1e-6); //coverageUncertainty
        assertEquals(0.00467707, results[3].doubleValue(), 1e-6); //analysisError
        assertEquals(0.5, results[4].doubleValue(), 1e-6); //seaIceFraction
    }


    @Test
    public void testReadSourceGrids() throws Exception {
        NetcdfFile l4File = TestL3ProductMaker.readL4GridsSetup();
        //execution
        Grid[] grids = fileType.readSourceGrids(l4File, SstDepth.skin);
        //verification
        assertEquals(3, grids.length);

        // analysed_sst
        assertEquals(2000, grids[0].getSampleInt(0, 0));
        assertEquals(293.14999344944954, grids[0].getSampleDouble(0, 0));
        assertEquals(2000, grids[0].getSampleInt(1, 0));
        assertEquals(293.14999344944954, grids[0].getSampleDouble(1, 0));
        // analysis_error
        assertEquals(-32768, grids[1].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[1].getSampleDouble(0, 0));
        assertEquals(-32768, grids[1].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[1].getSampleDouble(1, 0));
        // sea_ice_fraction
        assertEquals(-128, grids[2].getSampleInt(0, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(0, 0));
        assertEquals(-128, grids[2].getSampleInt(1, 0));
        assertEquals(Double.NaN, grids[2].getSampleDouble(1, 0));
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertFalse("Hallo".matches(fileType.getFilenameRegex()));
        assertFalse("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz".matches(fileType.getFilenameRegex()));
        assertFalse("19950723120045-ESACCI-L3C_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertFalse("20100701000000-ESACCI-L3U_GHRSST-SSTsubskin-AMSRE-LT-04.1-01.1.nc".matches(fileType.getFilenameRegex()));

        assertTrue("20100701000000-ESACCI-L4_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L4_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("19950723120045-ESACCI-L4_GHRSST-SSTdepth-AATSR-DM-v02.0-fv01.0.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L4_GHRSST-SSTfnd-ATSR1-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20121101000000-ESACCI-L4_GHRSST-SSTsubskin-ATSR2-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L4_GHRSST-SSTsubskin-AMSRE-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
        assertTrue("20100701000000-ESACCI-L4_GHRSST-SSTsubskin-SEVIRI_SST-LT-v04.1-fv01.1.nc".matches(fileType.getFilenameRegex()));
    }
}
