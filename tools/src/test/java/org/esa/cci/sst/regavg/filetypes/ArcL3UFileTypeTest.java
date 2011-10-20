package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.util.CellFactory;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.UTC;
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
        assertEquals("AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz", fileType.getFilenameRegex());
    }

    @Test
    public void testCell5Factory() throws Exception {
        ScalarCoverageUncertaintyProvider provider = new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5);
        CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(provider);
        assertNotNull(cell5Factory);
        AggregationCell5 cell5 = cell5Factory.createCell(52, 78);
        assertNotNull(cell5);
        assertEquals(52, cell5.getX());
        assertEquals(78, cell5.getY());
    }

    @Test
    public void testCell5Aggregation() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        AggregationCell5Context context = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 292.0),
                                                                              new ScalarGrid(sourceGridDef, 0.1),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 291.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));
        CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        AggregationCell5 cell5 = cell5Factory.createCell(0, 0);
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
        assertEquals(0.1, results[2].doubleValue(), 1e-6);
        assertEquals(1.2 * (1.0 - pow(expectedN / 77500.0, 0.5)), results[3].doubleValue(), 1e-6);
    }

    @Test
    public void testCell90Aggregation() throws Exception {
        CellFactory<AggregationCell90> cell90Factory = fileType.getCell90Factory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));
        AggregationCell90 cell90 = cell90Factory.createCell(0, 0);

        GridDef sourceGridDef = GridDef.createGlobal(0.1);
        AggregationCell5Context context = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 292.0),
                                                                              new ScalarGrid(sourceGridDef, 0.1),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 291.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));

        CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(new ScalarCoverageUncertaintyProvider(1.1, 1.2, 0.5));

        AggregationCell5 cell5_1 = cell5Factory.createCell(0, 0);
        cell5_1.accumulate(context, new Rectangle(0, 0, 10, 10));

        AggregationCell5 cell5_2 = cell5Factory.createCell(1, 0);
        cell5_2.accumulate(context, new Rectangle(0, 0, 10, 10));

        AggregationCell5 cell5_3 = cell5Factory.createCell(2, 0);
        cell5_3.accumulate(context, new Rectangle(0, 0, 10, 10));

        AggregationCell5 cell5_4 = cell5Factory.createCell(3, 0);
        cell5_4.accumulate(context, new Rectangle(0, 0, 10, 10));

        cell90.accumulate(cell5_1, 0.25); // --> 0.125
        cell90.accumulate(cell5_2, 0.5);  // --> 0.25
        cell90.accumulate(cell5_3, 0.25); // --> 0.125
        cell90.accumulate(cell5_4, 1.0);  // --> 0.5

        int expectedN = 4;
        assertEquals(expectedN, cell90.getSampleCount());
        Number[] results = cell90.getResults();
        assertNotNull(results);
        assertEquals(4, results.length);
        assertEquals(292.0, results[0].doubleValue(), 1e-6);
        assertEquals(0.5, results[1].doubleValue(), 1e-6);
        assertEquals(0.1, results[2].doubleValue(), 1e-6);
        assertEquals(1.1 / sqrt(expectedN), results[3].doubleValue(), 1e-6);
    }
}