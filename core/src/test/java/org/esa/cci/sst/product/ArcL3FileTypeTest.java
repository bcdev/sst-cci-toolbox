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
import org.esa.cci.sst.aggregate.*;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.GridDef;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.junit.Assert.*;

/**
 * Tests for ARC-L3 file type.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class ArcL3FileTypeTest {

    private static final FileType FILE_TYPE = ArcL3FileType.INSTANCE;

    @Test
    public void testParseDate() throws Exception {

        final File file = new File("AT2_AVG_3PAARC20020112_D_dN2b.nc.gz");
        assertEquals(TimeUtil.parseShortUtcFormat("2002-01-12"), FILE_TYPE.parseDate(file.getName()));
        final File file1 = new File("AT1_AVG_3PAARC20020416_D_dN2b.nc.gz");
        assertEquals(TimeUtil.parseShortUtcFormat("2002-04-16"), FILE_TYPE.parseDate(file1.getName()));
        final File file2 = new File("AT2_AVG_3PAARC20020120_D_nN2b.nc.gz");
        assertEquals(TimeUtil.parseShortUtcFormat("2002-01-20"), FILE_TYPE.parseDate(file2.getName()));
        final File file3 = new File("ATS_AVG_3PAARC20020915_D_nD3b.nc.gz");
        assertEquals(TimeUtil.parseShortUtcFormat("2002-09-15"), FILE_TYPE.parseDate(file3.getName()));

        try {
            final File file4 = new File("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz");
            FILE_TYPE.parseDate(file4.getName());
            fail();
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testFilenameRegex() throws Exception {
        assertEquals("AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?", FILE_TYPE.getFilenameRegex());
    }

    @Test
    public void testCell5Factory() throws Exception {
        final AggregationContext context = new AggregationContext();
        final CellFactory<? extends AggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);
        assertNotNull(cell5Factory);

        final AggregationCell cell5 = cell5Factory.createCell(52, 78);
        assertNotNull(cell5);
        assertEquals(52, cell5.getX());
        assertEquals(78, cell5.getY());
    }

    @Test
    public void testCell5Aggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 0.1));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);
        final SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(20, 0, 10, 10));

        final int expectedN = 3 * 10 * 10;
        assertEquals(expectedN, cell5.getSampleCount());

        final Number[] results = cell5.getResults();
        assertNotNull(results);

        assertEquals(8, results.length);
        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1e-6);
        assertEquals(sqrt(300 * sqr(0.1 * 0.8) / sqr(300 * 0.8)), results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(),
                1e-6);
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)),
                results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
    }

    @Test
    public void testCell90Aggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 0.1));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<CellAggregationCell<AggregationCell>> cell90Factory = FILE_TYPE.getCellFactory90(context);
        final CellAggregationCell<AggregationCell> cell90 = cell90Factory.createCell(0, 0);
        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);

        final SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
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

        final int expectedN90 = 4;
        assertEquals(expectedN90, cell90.getSampleCount());

        final Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1e-6);
        // TODO - replace inexplicable numbers by formulas, testCell5Aggregation()
        assertEquals(0.0046771, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(0.8673687, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
    }

    @Test
    public void testSameMonthAggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationFactory<SameMonthAggregation<AggregationCell>> aggregationFactory = FILE_TYPE.getSameMonthAggregationFactory();
        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 0.1));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);

        final SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 10, 10, 10));
        cell5_2.accumulate(context, new Rectangle(0, 10, 10, 10));

        final SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_4.accumulate(context, new Rectangle(10, 10, 10, 10));

        final SameMonthAggregation<AggregationCell> aggregation = aggregationFactory.createAggregation();

        aggregation.accumulate(cell5_1, 0.25);
        aggregation.accumulate(cell5_2, 0.5);
        aggregation.accumulate(cell5_3, 0.25);
        aggregation.accumulate(cell5_4, 1.0);

        final int expectedN = 4;
        assertEquals(expectedN, aggregation.getSampleCount());

        final Number[] results = aggregation.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1e-6);
        // TODO - replace inexplicable numbers by formulas, testCell5Aggregation()
        assertEquals(0.00414578, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(0.66611642, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
    }

    @Test
    public void testMultiMonthAggregation() throws Exception {
        final GridDef sourceGridDef = FILE_TYPE.getGridDef();
        final AggregationFactory<SameMonthAggregation<AggregationCell>> sameMonthAggregationFactory = FILE_TYPE.getSameMonthAggregationFactory();
        final AggregationFactory<MultiMonthAggregation<RegionalAggregation>> multiMonthAggregationFactory = FILE_TYPE.getMultiMonthAggregationFactory();

        final AggregationContext context = new AggregationContext();
        context.setSstGrid(new ScalarGrid(sourceGridDef, 292.0));
        context.setRandomUncertaintyGrid(new ScalarGrid(sourceGridDef, 0.1));
        context.setClimatologySstGrid(new ScalarGrid(sourceGridDef, 291.5));
        context.setSeaCoverageGrid(new ScalarGrid(sourceGridDef, 0.8));
        context.setCoverageUncertaintyProvider(new MockCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        final CellFactory<SpatialAggregationCell> cell5Factory = FILE_TYPE.getCellFactory5(context);
        final SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 10, 10, 10));
        cell5_2.accumulate(context, new Rectangle(0, 10, 10, 10));

        final SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        final SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_4.accumulate(context, new Rectangle(10, 10, 10, 10));

        final SameMonthAggregation<AggregationCell> aggregation1 = sameMonthAggregationFactory.createAggregation();
        final SameMonthAggregation<AggregationCell> aggregation2 = sameMonthAggregationFactory.createAggregation();

        aggregation1.accumulate(cell5_1, 0.25);
        aggregation1.accumulate(cell5_2, 0.5);

        aggregation2.accumulate(cell5_3, 0.25);
        aggregation2.accumulate(cell5_4, 1.0);

        final MultiMonthAggregation<RegionalAggregation> aggregation3 = multiMonthAggregationFactory.createAggregation();
        aggregation3.accumulate(aggregation1);
        aggregation3.accumulate(aggregation2);

        final int expectedN = 2;
        assertEquals(expectedN, aggregation3.getSampleCount());

        final Number[] results = aggregation3.getResults();
        assertNotNull(results);
        assertEquals(8, results.length);

        assertEquals(292.0, results[Aggregation.SST].doubleValue(), 1e-6);
        assertEquals(0.5, results[Aggregation.SST_ANOMALY].doubleValue(), 1e-6);
        // TODO - replace inexplicable numbers by formulas, testCell5Aggregation()
        assertEquals(0.00381517, results[Aggregation.RANDOM_UNCERTAINTY].doubleValue(), 1e-6);
        assertEquals(0.62927276, results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue(), 1e-6);
    }

    private static double sqr(double x) {
        return x * x;
    }
}