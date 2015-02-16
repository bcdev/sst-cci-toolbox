/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.product;

import org.esa.cci.sst.ScalarGrid;
import org.esa.cci.sst.TestL3ProductMaker;
import org.esa.cci.sst.aggregate.Aggregation;
import org.esa.cci.sst.aggregate.AggregationCell;
import org.esa.cci.sst.aggregate.AggregationContext;
import org.esa.cci.sst.aggregate.SpatialAggregationCell;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.Grid;
import org.esa.cci.sst.grid.GridDef;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.awt.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.junit.Assert.*;

/**
 * {@author Bettina Scholze}
 * Date: 17.09.12 11:02
 */
public class CciL4FileTypeTest {
    private static final FileType FILE_TYPE = CciL4FileType.INSTANCE;

    @Test
    public void testL3UCell5Aggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 1.0));
        context.setSeaIceFractionGrid(new ScalarGrid(sourceGridDef, 0.5));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);
        final SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);

        //execution
        cell5.accumulate(context, new Rectangle(0, 0, 100, 100));

        final int expectedN = 100 * 100;
        assertEquals(expectedN, cell5.getSampleCount());

        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);
        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1.0e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1.0e-6);
        assertEquals((0.5 * 10000) / 10000, results[Aggregation.SEA_ICE_FRACTION].doubleValue(), 1e-6);
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)), results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(sqrt((0.8 * 0.8 * 10000) / ((0.8 * 10000) * (0.8 * 10000))), results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
    }

    @Test
    public void testCell90Aggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 0.1));
        context.setSeaIceFractionGrid(new ScalarGrid(sourceGridDef, 0.5));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<CellAggregationCell<AggregationCell>> cell90Factory = FILE_TYPE.getCellFactory90(context);
        final CellAggregationCell<AggregationCell> cell90 = cell90Factory.createCell(0, 0);
        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);

        final SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 100, 100));

        final SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(100, 0, 100, 100));

        final SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(200, 0, 100, 100));

        final SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(300, 0, 100, 100));

        final int expectedN5_1 = 10000;
        final int expectedN5_2 = 10000;
        final int expectedN5_3 = 10000;
        final int expectedN5_4 = 10000;
        assertEquals(expectedN5_1, cell5_1.getSampleCount());
        assertEquals(expectedN5_2, cell5_2.getSampleCount());
        assertEquals(expectedN5_3, cell5_3.getSampleCount());
        assertEquals(expectedN5_4, cell5_4.getSampleCount());

        cell90.accumulate(cell5_1, 0.25); // --> w=0.125, n = 100
        cell90.accumulate(cell5_2, 0.5);  // --> w=0.25, n = 100
        cell90.accumulate(cell5_3, 0.25); // --> w=0.125, n = 100
        cell90.accumulate(cell5_4, 1.0);  // --> w=0.5, n = 200

        final int expectedN90 = 4;
        assertEquals(expectedN90, cell90.getSampleCount());

        final Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1.0e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1.0e-6);
        // todo - replace inexplicable numbers by formulas, testCell5Aggregation() (nf)
        assertEquals(0.5, results[Aggregation.SEA_ICE_FRACTION].doubleValue(), 1e-6);
        assertEquals(0.7111627589581172, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(5.86301969977808E-4, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
    }


    @Test
    public void testReadSourceGrids() throws Exception {
        NetcdfFile l4File = TestL3ProductMaker.readL4GridsSetup();
        //execution
        final AggregationContext context = FILE_TYPE.readSourceGrids(l4File, SstDepth.skin, new AggregationContext());

        // analysed_sst
        final Grid sstGrid = context.getSstGrid();
        assertEquals(2000, sstGrid.getSampleInt(0, 3599));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(0, 3599), 1e-8);
        assertEquals(2000, sstGrid.getSampleInt(1, 3599));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(1, 3599), 1e-8);

        // analysis_error
        final Grid randomUncertaintyGrid = context.getRandomUncertaintyGrid();
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(0, 3599));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(0, 3599)));
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(1, 3599));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(1, 3599)));

        // sea_ice_fraction
        final Grid seaIceFractionGrid = context.getSeaIceFractionGrid();
        assertEquals(-128, seaIceFractionGrid.getSampleInt(0, 3599));
        assertTrue(Double.isNaN(seaIceFractionGrid.getSampleDouble(0, 3599)));
        assertEquals(-128, seaIceFractionGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(seaIceFractionGrid.getSampleDouble(1, 3599)));
    }

    @Test
    public void testFileNameRegex() throws Exception {
        assertTrue("20000701120000-ESACCI-L4_GHRSST-SSTdepth-OSTIA-GLOB_LT-v02.0-fv01.0.nc".matches(FILE_TYPE.getFilenameRegex()));
        assertTrue("20000701120000-ESACCI-L4_GHRSST-SSTskin-OSTIA-GLOB_LT-v02.0-fv01.0.nc".matches(FILE_TYPE.getFilenameRegex()));
        assertTrue("20000701120000-ESACCI-L4_GHRSST-SSTsubskin-OSTIA-GLOB_LT-v02.0-fv01.0.nc".matches(FILE_TYPE.getFilenameRegex()));
        assertTrue("20000701120000-ESACCI-L4_GHRSST-SSTfnd-OSTIA-GLOB_LT-v02.0-fv01.0.nc".matches(FILE_TYPE.getFilenameRegex()));
    }
}
