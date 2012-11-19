package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 16:06
 */
public class LutForXTimeTest {

    @Test
    public void testReadInAndFlip() throws Exception {
        File file;
        try {
            file = new File("./src/main/conf/auxdata/x0_time.txt");
        } catch (Exception e) {
            throw new Exception("Get x0_time.txt from <fs1:/projects/ongoing/SST-CCI/docs/technical-specification/tools> and put it in <./src/main/conf/auxdata/x0_time.txt>");
        }
        ArrayGrid lut = LutForXTime.readInAndFlip(file, SpatialResolution.DEGREE_2_00, -32768.0);

        assertEquals(4.99, lut.getSampleDouble(0, 0));
        assertTrue(Double.isNaN(lut.getSampleDouble(1, 0)));
        assertEquals(4.11, lut.getSampleDouble(0, 12));
        assertEquals(3.72, lut.getSampleDouble(1, 13));
        assertEquals(2.81, lut.getSampleDouble(5, 24));
    }

    @Test
    public void testInterpolateTo005Degree() throws Exception {
        File file;
        try {
            file = new File("./src/main/conf/auxdata/x0_time.txt");
        } catch (Exception e) {
            throw new Exception("Get x0_time.txt from <fs1:/projects/ongoing/SST-CCI/docs/technical-specification/tools> and put it in <./src/main/conf/auxdata/x0_time.txt>");
        }
        ArrayGrid lut = LutForXTime.readInAndFlip(file, SpatialResolution.DEGREE_2_00, -32768.0);

        //execution
        ArrayGrid lut005 = LutForXTime.interpolateTo005(lut);

        assertEquals(3600, lut005.getHeight());
        assertEquals(7200, lut005.getWidth());
        assertEquals(1.0, lut005.getScaling());
        assertEquals(0.0, lut005.getOffset());

        assertEquals(Double.NaN, lut005.getSampleDouble(0, 0));
        assertEquals(Double.NaN, lut005.getSampleDouble(0, 3559));
        assertEquals(4.99, lut005.getSampleDouble(1, 140));

        try {
            lut005.getSampleDouble(0, 3600);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        assertEquals(Double.NaN, lut005.getSampleDouble(7199, 3559));
        assertEquals(2.00478125, lut005.getSampleDouble(7019, 2819));
        assertEquals(2.18213125, lut005.getSampleDouble(7019, 2719));
        try {
            lut005.getSampleDouble(7200, 3559);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }
    }
}
