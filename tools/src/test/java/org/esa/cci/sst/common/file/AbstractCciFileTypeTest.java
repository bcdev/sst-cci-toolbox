package org.esa.cci.sst.common.file;

import org.esa.cci.sst.aggregate.*;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.common.*;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.file.AbstractCciFileType;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.GridDef;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@author Bettina Scholze}
 * Date: 17.09.12 10:52
 */
public class AbstractCciFileTypeTest {

    final FileType fileType = newFileTypeInstance();

    @Test
    public void testParseDate() throws Exception {
        final File file = new File("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
        assertEquals(TimeUtil.parseShortUtcFormat("2010-07-01"),
                fileType.parseDate(file.getName()));
        final File file1 = new File("20121201000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
        assertEquals(TimeUtil.parseShortUtcFormat("2012-12-01"),
                fileType.parseDate(file1.getName()));
        final File file2 = new File("19950731000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
        assertEquals(TimeUtil.parseShortUtcFormat("1995-07-31"),
                fileType.parseDate(file2.getName()));
        final File file3 = new File("19950731000000-ESACCI-L4_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
        assertEquals(TimeUtil.parseShortUtcFormat("1995-07-31"),
                fileType.parseDate(file3.getName()));

        try {
            final File file4 = new File("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz");
            fileType.parseDate(file4.getName());
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }

        try {
            final File file4 = new File("A20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
            fileType.parseDate(file4.getName());
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }
    }

    @Test
    public void testGetRdac() throws Exception {
        assertEquals("ESACCI", fileType.getRdac());
    }

    @Test
    public void testGetGridDef() throws Exception {
        GridDef gridDef = fileType.getGridDef();
        assertEquals(25920000, gridDef.getWidth() * gridDef.getHeight() * gridDef.getTime());
        assertEquals(0.05, gridDef.getResolution());
    }

    private AbstractCciFileType newFileTypeInstance() {
        return new AbstractCciFileType() {
            @Override
            public String getFilenameRegex() {
                return null;
            }

            @Override
            public AggregationContext readSourceGrids(NetcdfFile datafile, SstDepth sstDepth,
                                                      AggregationContext context) throws IOException {
                return context;
            }

            @Override
            public Variable[] addResultVariables(NetcdfFileWriteable datafile, Dimension[] dims, SstDepth sstDepth) {
                return new Variable[0];
            }

            @Override
            public AggregationFactory<SameMonthAggregation<AggregationCell>> getSameMonthAggregationFactory() {
                return null;
            }

            @Override
            public AggregationFactory<MultiMonthAggregation<RegionalAggregation>> getMultiMonthAggregationFactory() {
                return null;
            }

            @Override
            public CellFactory<SpatialAggregationCell> getCellFactory5(AggregationContext context) {
                return null;
            }

            @Override
            public CellFactory<CellAggregationCell<AggregationCell>> getCellFactory90(AggregationContext context) {
                return null;
            }

        };
    }
}
