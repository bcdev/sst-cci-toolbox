package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.UTC;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
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
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        assertEquals(format.parse("2010-07-01"),
                     fileType.parseDate(new File("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));
        assertEquals(format.parse("2012-12-01"),
                     fileType.parseDate(new File("20121201000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));
        assertEquals(format.parse("1995-07-31"),
                     fileType.parseDate(new File("19950731000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));
        assertEquals(format.parse("1995-07-31"),
                     fileType.parseDate(new File("19950731000000-ESACCI-L4_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc")));

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
            public AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth,
                                                      AggregationContext context) throws IOException {
                return context;
            }

            @Override
            public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth,
                                                    boolean totalUncertainty, Dimension[] dims) {
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

            @Override
            public boolean hasSynopticUncertainties() {
                return false;
            }
        };
    }
}
