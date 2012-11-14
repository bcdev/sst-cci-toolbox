package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.regrid.SpatialResolution;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 16:06
 */
public class LutForXTimeTest {

    @Test
    public void testLutSameResolution() throws Exception {
        File file = new File("./src/main/conf/auxdata/x0_time.txt");
        LutForXTime lutForXTime = LutForXTime.read(file, SpatialResolution.DEGREE_2_00, -32768.0);

        assertEquals(4.99, lutForXTime.getXTime(0, 0));
        assertTrue(Double.isNaN(lutForXTime.getXTime(1, 0)));
    }


}
