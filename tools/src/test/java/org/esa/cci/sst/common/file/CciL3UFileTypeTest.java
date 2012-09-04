package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import static junit.framework.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 14:32
 */
public class CciL3UFileTypeTest {

    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        CciL3UFileType cciL3UFileType = new CciL3UFileType();
        //execution
        Grid[] grids = cciL3UFileType.readSourceGrids(l3UFile, SstDepth.skin);
        //verification
        assertEquals(5, grids.length);

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
    }
}
