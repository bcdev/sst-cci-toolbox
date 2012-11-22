package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.junit.Before;
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

    private File file;

    @Before
    public void setUp() throws Exception {
        file = new File("./src/main/conf/auxdata/x0_time.txt");
    }

    @Test
    public void testCalculateCorner() throws Exception {
        assertEquals(0, LutForXTimeSpace.calculateCorner(0.025));
        assertEquals(0, LutForXTimeSpace.calculateCorner(1));
        assertEquals(0, LutForXTimeSpace.calculateCorner(2));
        assertEquals(0, LutForXTimeSpace.calculateCorner(2.5));
        assertEquals(0, LutForXTimeSpace.calculateCorner(2.975));

        assertEquals(1, LutForXTimeSpace.calculateCorner(3.0));
        assertEquals(1, LutForXTimeSpace.calculateCorner(3.025));
        assertEquals(1, LutForXTimeSpace.calculateCorner(3.5));

        assertEquals(2, LutForXTimeSpace.calculateCorner(5.5));

        assertEquals(179, LutForXTimeSpace.calculateCorner(360));
    }

    @Test
    public void testRead() throws Exception {
        SpatialResolution degree1000 = SpatialResolution.DEGREE_10_00;
        GridDef gridDef = degree1000.getAssociatedGridDef();
        LutForXTimeSpace lut = LutForXTimeSpace.read(file, degree1000, -32768.0);

        String expected = "" +
                "4,99 4,99 4,93 4,99 4,99 4,99 4,99 4,99 4,99 4,70 4,68 4,98 4,99 4,99 4,99 4,91 4,81 3,76 2,95 3,17 3,37 3,65 3,77 4,03 4,15 4,30 4,89 4,94 4,97 4,91 4,95 4,98 4,97 4,98 4,99 4,71 \n" +
                "4,98 4,98 4,99 4,99 4,99 4,93 4,88 4,95 4,99 4,36 3,75 4,21 3,15  NaN  NaN 2,51 2,73 2,65 2,81 2,99 2,65 2,72 2,86 3,31 4,31 4,83 4,98 4,97 4,92 4,83 4,87 4,83 4,93 4,98 4,99 4,99 \n" +
                "4,00 4,41 2,91 3,58 4,79 4,99 4,88 4,97 4,37 4,25 3,50 2,96 2,75 3,03 3,10 3,13 2,80 2,72 2,85 3,08 3,29 3,07 3,41 3,64 4,91 4,63  NaN  NaN  NaN  NaN  NaN  NaN  NaN 3,40 3,86 3,75 \n" +
                "2,76 3,05 2,98 2,81 2,86 3,02  NaN  NaN 4,29 4,12 3,53 2,56 2,45 2,66 3,26 3,50 3,15 3,24 3,16 3,36 3,63  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN 4,02 3,57 3,07 2,92 2,55 \n" +
                "2,70 2,91 3,13 3,04 2,87 2,78  NaN  NaN  NaN  NaN 2,71 2,64 2,56 2,70 2,86 3,10 2,94 3,24 3,06 3,12 3,53 3,74 3,64 3,75  NaN  NaN  NaN  NaN  NaN 2,79 2,79 2,77 2,74 2,72 2,85 2,86 \n" +
                "3,26 3,16 3,12 3,16 3,15 2,74 2,80  NaN 3,85 3,69 2,94 2,73 2,57 2,79 2,84 2,89 2,75 2,76 3,01 2,94 2,89 3,03 2,98 2,97  NaN  NaN  NaN  NaN  NaN 2,57 2,68 2,92 2,96 2,83 3,05 3,23 \n" +
                "2,61 2,89 3,17 3,24 3,21 2,70 2,95 3,12 3,00 3,17 2,89 2,70 2,38 2,32 2,41 2,33 2,46 2,99  NaN  NaN  NaN 3,09 2,71 2,87 2,72 3,06 3,00 3,11 3,12 2,95 3,21 3,26 3,01 2,64 2,65 2,72 \n" +
                "1,93 2,20 2,51 2,50 2,57 2,45 2,54 2,91 3,11 2,36 2,34 2,29 2,14 2,21 2,03 1,90 2,37  NaN  NaN  NaN  NaN 2,48 2,52 2,76 2,46 2,68 2,98 3,54 3,25 3,09 2,95 2,68 2,21 2,11 2,07 1,92 \n" +
                "2,21 2,46 2,69 2,78 2,78 2,73 2,69 2,53 2,50 2,70 2,50 2,29 2,36 2,29 2,18 2,13 2,07 2,25 2,22  NaN  NaN  NaN 2,75 2,74 2,38 2,54 2,90 2,76 2,78 2,62 2,33 2,43 2,17 2,04 2,07 2,19 \n" +
                "2,31 2,35 2,62 2,83 3,10 3,15 3,22 3,05 2,98 3,16 2,80  NaN 2,81 1,98 1,93 1,99 2,17 2,36 2,42 2,44  NaN 2,46 2,45 2,73 2,66 2,56 2,50 2,64 2,63 2,45 2,32 2,47 2,73 2,32 2,30 2,24 \n" +
                "2,31 2,47 2,31 2,11 2,37 2,54 2,48 2,42 2,68 2,75 2,73  NaN  NaN 2,68 1,98 2,25 2,32 2,38 2,56 2,85  NaN 2,86 2,57 2,73 2,70 2,66 2,57 2,37 2,41 2,53 2,63 2,64 2,90 2,84 2,63 2,45 \n" +
                "2,90 2,78 2,81 2,81 2,92 2,85 2,85 2,94 2,79 2,69 2,78  NaN 3,55 2,99 2,84 3,02 3,13 3,15 2,65 2,77  NaN 2,99 3,06 3,44 3,51 3,55 3,29 3,00 2,69 2,59  NaN  NaN 3,37 3,12 2,91 2,87 \n" +
                "2,80 2,77 2,79 2,96 3,27 3,18 3,03 2,92 2,87 2,54 2,57 2,86 2,76 3,11 2,93 2,88 2,82 2,74 2,71 3,17 2,98 2,82 2,75 2,85 2,83 2,94 2,99 3,09 2,94 2,74 2,35 2,52 2,60 3,27 2,82 2,81 \n" +
                "2,84 2,64 2,56 2,62 2,91 2,70 2,65 2,54 2,48 2,38 2,47 2,60 2,71 2,90 3,13 2,78 2,65 2,52 2,27 2,49 2,67 2,46 2,33 2,50 2,72 2,60 2,50 2,43 2,47 2,66 2,51 2,37 2,53 2,61 2,54 2,47 \n" +
                "2,51 2,60 2,51 2,68 2,64 2,60 2,33 2,28 2,22 2,29 2,43 2,64 2,47 2,38 2,34 2,25 2,35 2,27 2,21 2,20 2,13 1,88 1,95 2,02 2,16 2,04 2,26 2,18 2,29 2,38 2,36 2,10 2,28 2,36 2,24 2,19 \n" +
                "3,09 3,19 3,01 2,98 2,92 2,63 2,50 2,61 2,66 2,60 2,55 2,59 2,74 3,72 3,29 3,25 3,33 3,11 2,77 2,84 2,68 2,55 2,78 2,51 3,33 3,52 2,79 2,48 2,64 2,52 2,81 2,42 2,34 2,75 3,09 3,17 \n" +
                "4,86 4,98 4,97 4,57 4,31 3,17 3,29 3,33 2,68 3,44 3,79 3,95 3,86 4,37 4,90 4,69 4,14 3,24 2,94 3,21 3,71  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN 4,71 4,73 \n" +
                " NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN \n";

        StringBuilder stringBuilder = new StringBuilder();
        for (int y = 0; y < gridDef.getHeight(); y++) {
            for (int x = 0; x < gridDef.getWidth(); x++) {
                double xTime = lut.getXValue(x, y);
                stringBuilder.append(String.format("%4.2f ", xTime));

            }
            stringBuilder.append("\n");
        }
        assertEquals(expected, stringBuilder.toString());
    }

    @Test
    public void testReadInAndFlip() throws Exception {
        ArrayGrid lut = LutForXTimeSpace.readInAndFlip(file, -32768.0);

        assertEquals(10.0, lut.getSampleDouble(0, 0));
        assertEquals(1.0, lut.getSampleDouble(1, 0));
        assertEquals(4.11, lut.getSampleDouble(0, 12));
        assertEquals(3.72, lut.getSampleDouble(1, 13));
        assertEquals(2.81, lut.getSampleDouble(5, 24));
    }

    @Test
    public void testInterpolateTo005Degree() throws Exception {
        ArrayGrid lut = LutForXTimeSpace.readInAndFlip(file, -32768.0);

        //execution
        ArrayGrid lut005 = LutForXTimeSpace.interpolateTo005(lut);

        assertEquals(3600, lut005.getHeight());
        assertEquals(7200, lut005.getWidth());
        assertEquals(1.0, lut005.getScaling());
        assertEquals(0.0, lut005.getOffset());

        int boundary = 20 - 1;
        int boxExtend = 40;

        //lower-left corner in LUT file (now in the fine resolution and upper-left)
        assertEquals("boundary", 10.0, lut005.getSampleDouble(0, 0), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(1, 1), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(2, 2), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(3, 3), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(9, 9), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(10, 10), 0.0);
        assertEquals("boundary", 10.0, lut005.getSampleDouble(19, 19), 0.0);
        assertEquals("closest to upper-left value of corner", 9.776, lut005.getSampleDouble(20, 20), 1.0e-3);
        assertEquals("closest to lower-right value of corner", 1.001, lut005.getSampleDouble(59, 59), 1.0e-3);
        assertTrue("next corners' box", Double.isNaN(lut005.getSampleDouble(60, 60)));

        assertEquals(4.99, lut005.getSampleDouble(1, 140));

        //upper-left corner in LUT file (now in the fine resolution and lower-left)
        assertEquals(Double.NaN, lut005.getSampleDouble(0, 3559));
        try {
            lut005.getSampleDouble(0, 3600);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        //upper-right corner in LUT file (now in the fine resolution and lower-right)
        assertEquals(Double.NaN, lut005.getSampleDouble(7199, 3559));
        assertEquals(2.002, lut005.getSampleDouble(7019, 2819), 1.0e-3);
        assertEquals(2.205, lut005.getSampleDouble(7019, 2719), 1.0e-3);
        try {
            lut005.getSampleDouble(7200, 3559);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }

        //lower-right corner in LUT file (now in the fine resolution and upper-right)
        assertEquals("boundary" ,10.0, lut005.getSampleDouble(7199, 0));
        //in the middle of the lower-right corner points
        assertEquals("middle of the corners' box", 3.363, lut005.getSampleDouble(7199 - boundary - boxExtend / 2, 0 + boundary + boxExtend / 2), 1.0e-3);
        assertEquals("closest to the 10.0 corner", 9.887, lut005.getSampleDouble(7199 - boundary - 1, 0 + boundary), 1.0e-3);
        assertEquals("closest to a 1.0 corner",1.112, lut005.getSampleDouble(7199 - boundary - boxExtend, 0 + boundary), 1.0e-3);
        assertEquals("closest to a 1.0 corner", 1.113, lut005.getSampleDouble(7199 - boundary, 0 + boundary + boxExtend), 1.0e-3);
        assertEquals("closest to a 1.0 corner", 1.001, lut005.getSampleDouble(7199 - boundary - boxExtend, 0 + boundary + boxExtend), 1.0e-3);
    }
}
