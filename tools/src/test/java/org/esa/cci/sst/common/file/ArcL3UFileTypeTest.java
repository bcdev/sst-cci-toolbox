package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.ScalarGrid;
import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.AggregationCell90;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.UTC;
import org.esa.cci.sst.util.calculators.ScalarCoverageUncertaintyProvider;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ArcL3UFileTypeTest {
    FileType fileType = ArcL3UFileType.INSTANCE;

    @Test
    public void testParseDate() throws Exception {
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        assertEquals(format.parse("2002-01-12"), fileType.parseDate(new File("AT2_AVG_3PAARC20020112_D_dN2b.nc.gz")));  // day/nadir
        assertEquals(format.parse("2002-04-16"), fileType.parseDate(new File("AT1_AVG_3PAARC20020416_D_dN2b.nc.gz")));  // day/dual
        assertEquals(format.parse("2002-01-20"), fileType.parseDate(new File("AT2_AVG_3PAARC20020120_D_nN2b.nc.gz")));  // night/nadir
        assertEquals(format.parse("2002-09-15"), fileType.parseDate(new File("ATS_AVG_3PAARC20020915_D_nD3b.nc.gz")));  // night/dual

        try {
            fileType.parseDate(new File("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz"));
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }
    }

    @Test
    public void testOtherProperties() throws Exception {
        assertEquals(ProcessingLevel.L3U, fileType.getProcessingLevel());
        assertEquals("AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?", fileType.getFilenameRegex());
    }

    @Test
    public void testCell5Factory() throws Exception {
        ScalarCoverageUncertaintyProvider provider = new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5);
        CellFactory<? extends AggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(provider);
        assertNotNull(cell5Factory);
        AggregationCell cell5 = cell5Factory.createCell(52, 78);
        assertNotNull(cell5);
        assertEquals(52, cell5.getX());
        assertEquals(78, cell5.getY());
    }

    @Test
    public void testCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0),
                        new ScalarGrid(sourceGridDef, 0.1),
                },
                new ScalarGrid(sourceGridDef, 291.5),
                new ScalarGrid(sourceGridDef, 0.8));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        SpatialAggregationCell cell5 = cell5Factory.createCell(0, 0);
        cell5.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5.accumulate(context, new Rectangle(20, 0, 10, 10));

        int expectedN = 3 * 10 * 10;
        assertEquals(expectedN, cell5.getSampleCount());
        Number[] results = cell5.getResults();
        assertNotNull(results);
        assertEquals(4, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6);
        assertEquals(0.5, results[1].doubleValue(), 1e-6);
        assertEquals(sqrt(300 * sqr(0.1 * 0.8) / sqr(300 * 0.8)), results[2].doubleValue(), 1e-6);
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)), results[3].doubleValue(), 1e-6);
    }

    @Test
    public void testCell90Aggregation() throws Exception {
        CellFactory<AggregationCell90> cell90Factory = fileType.getCell90Factory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));
        AggregationCell90 cell90 = cell90Factory.createCell(0, 0);

        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0),
                        new ScalarGrid(sourceGridDef, 0.1),
                },
                new ScalarGrid(sourceGridDef, 291.5),
                new ScalarGrid(sourceGridDef, 0.8));

        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

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
        assertEquals(4, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6);
        assertEquals(0.5, results[1].doubleValue(), 1e-6);
        // todo - replace inexplicable numbers by formulas, testCell5Aggregation() (nf)
        assertEquals(0.0046771, results[2].doubleValue(), 1e-6);
        assertEquals(0.8673687, results[3].doubleValue(), 1e-6);
    }

    @Test
    public void testSameMonthAggregation() throws Exception {
        AggregationFactory<SameMonthAggregation> sameMonthAggregationFactory = fileType.getSameMonthAggregationFactory();

        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0),
                        new ScalarGrid(sourceGridDef, 0.1),
                },
                new ScalarGrid(sourceGridDef, 291.5),
                new ScalarGrid(sourceGridDef, 0.8));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 10, 10, 10));
        cell5_2.accumulate(context, new Rectangle(0, 10, 10, 10));

        SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_4.accumulate(context, new Rectangle(10, 10, 10, 10));

        SameMonthAggregation aggregation = sameMonthAggregationFactory.createAggregation();

        aggregation.accumulate(cell5_1, 0.25);
        aggregation.accumulate(cell5_2, 0.5);
        aggregation.accumulate(cell5_3, 0.25);
        aggregation.accumulate(cell5_4, 1.0);

        int expectedN = 4;
        assertEquals(expectedN, aggregation.getSampleCount());
        Number[] results = aggregation.getResults();
        assertNotNull(results);
        assertEquals(4, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6);
        assertEquals(0.5, results[1].doubleValue(), 1e-6);
        // todo - replace inexplicable numbers by formulas, testCell5Aggregation() (nf)
        assertEquals(0.00414578, results[2].doubleValue(), 1e-6);
        assertEquals(0.66611642, results[3].doubleValue(), 1e-6);
    }


    @Test
    public void testMultiMonthAggregation() throws Exception {
        AggregationFactory<SameMonthAggregation> sameMonthAggregationFactory = fileType.getSameMonthAggregationFactory();
        AggregationFactory<MultiMonthAggregation> multiMonthAggregationFactory = fileType.getMultiMonthAggregationFactory();

        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        SpatialAggregationContext context = new SpatialAggregationContext(sourceGridDef,
                new Grid[]{
                        new ScalarGrid(sourceGridDef, 292.0),
                        new ScalarGrid(sourceGridDef, 0.1),
                },
                new ScalarGrid(sourceGridDef, 291.5),
                new ScalarGrid(sourceGridDef, 0.8));
        CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        SpatialAggregationCell cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 0, 10, 10));
        cell5_2.accumulate(context, new Rectangle(10, 10, 10, 10));
        cell5_2.accumulate(context, new Rectangle(0, 10, 10, 10));

        SpatialAggregationCell cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        SpatialAggregationCell cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));
        cell5_4.accumulate(context, new Rectangle(10, 10, 10, 10));

        SameMonthAggregation aggregation1 = sameMonthAggregationFactory.createAggregation();
        SameMonthAggregation aggregation2 = sameMonthAggregationFactory.createAggregation();

        aggregation1.accumulate(cell5_1, 0.25);
        aggregation1.accumulate(cell5_2, 0.5);

        aggregation2.accumulate(cell5_3, 0.25);
        aggregation2.accumulate(cell5_4, 1.0);

        MultiMonthAggregation aggregation3 = multiMonthAggregationFactory.createAggregation();
        aggregation3.accumulate(aggregation1);
        aggregation3.accumulate(aggregation2);

        int expectedN = 2;
        assertEquals(expectedN, aggregation3.getSampleCount());
        Number[] results = aggregation3.getResults();
        assertNotNull(results);
        assertEquals(4, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6);
        assertEquals(0.5, results[1].doubleValue(), 1e-6);
        // todo - replace inexplicable numbers by formulas, testCell5Aggregation() (nf)
        assertEquals(0.00381517, results[2].doubleValue(), 1e-6);
        assertEquals(0.62927276, results[3].doubleValue(), 1e-6);
    }

    public static double sqr(double x) {
        return x * x;
    }
}