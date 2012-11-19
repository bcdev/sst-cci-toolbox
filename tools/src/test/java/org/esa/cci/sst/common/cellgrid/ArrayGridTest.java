package org.esa.cci.sst.common.cellgrid;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static java.lang.Double.NaN;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ArrayGridTest {

    private static final double[] input = new double[]{
            999, 0.2, 999, 0.4, 0.1, 0.2, 999, 0.4,
            999, 0.3, 999, 999, 0.2, 0.3, 0.4, 999,
            0.3, 999, 0.1, 0.2, 0.3, 0.4, 999, 999,
            999, 0.1, 0.2, 0.3, 0.4, 0.1, 999, 999,
    };
    private GridDef gridDef;
    private ArrayGrid arrayGrid;


    @Before
    public void setUp() throws Exception {
        gridDef = GridDef.createGlobalGrid(8, 4);
        int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        Array array = Array.factory(DataType.DOUBLE, shape, input);
        arrayGrid = new ArrayGrid(gridDef, array, 999, 1.0, 0.0);
    }

    @Test
    public void testScaleDown() throws Exception {
        ArrayGrid arrayGrid2 = arrayGrid.scaleDown(2, 2);
        assertNotNull(arrayGrid2);
        assertEquals(gridDef.getWidth() / 2, arrayGrid2.getGridDef().getWidth());
        assertEquals(gridDef.getHeight() / 2, arrayGrid2.getGridDef().getHeight());
        assertEquals(gridDef.getEasting(), arrayGrid2.getGridDef().getEasting(), 1e-10);
        assertEquals(gridDef.getNorthing(), arrayGrid2.getGridDef().getNorthing(), 1e-10);
        assertEquals(gridDef.getResolutionX() * 2, arrayGrid2.getGridDef().getResolutionX(), 1e-10);
        assertEquals(gridDef.getResolutionY() * 2, arrayGrid2.getGridDef().getResolutionY(), 1e-10);
        double[] expected = new double[]{
                0.25, 0.4, 0.2, 0.4,
                0.2, 0.2, 0.3, NaN,
        };
        double[] actual = (double[]) arrayGrid2.getArray().getStorage();
        assertArrayEquals(expected, actual, 1e-10);
    }

    @Test
    public void testFillValue() throws Exception {

        double[] expected = new double[]{
                NaN, 0.2, NaN, 0.4, 0.1, 0.2, NaN, 0.4,
                NaN, 0.3, NaN, NaN, 0.2, 0.3, 0.4, NaN,
                0.3, NaN, 0.1, 0.2, 0.3, 0.4, NaN, NaN,
                NaN, 0.1, 0.2, 0.3, 0.4, 0.1, NaN, NaN,
        };

        double[] actual = new double[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = arrayGrid.getSampleDouble(i % 8, i / 8);
        }
        assertArrayEquals(expected, actual, 1e-10);
    }

    @Test
    public void testUnmask() throws Exception {
        int B1 = 0x0001;
        int B2 = 0x0002;
        int B3 = 0x0004;
        int B4 = 0x0008;
        Array array = Array.factory(DataType.INT, new int[]{4, 8}, new int[]{
                B1 | B2, B1 | B3, B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2,
                B1 | B3, B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2,
                B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2, B1 | B3,
                B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2, B1 | B3, B1 | B4,
        });
        ArrayGrid maskGrid = new ArrayGrid(gridDef, array, null, 1, 0);
        ArrayGrid unmaskedGrid = maskGrid.unmask(0x01);
        double[] expected = new double[]{
                1, 1, 1, 1, 0, 0, 0, 1,
                1, 1, 1, 0, 0, 0, 1, 1,
                1, 1, 0, 0, 0, 1, 1, 1,
                1, 0, 0, 0, 1, 1, 1, 1,
        };
        double[] actual = new double[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = unmaskedGrid.getSampleDouble(i % 8, i / 8);
        }
        assertArrayEquals(expected, actual, 1e-10);
    }

    @Test
    public void testGetSample() throws Exception {
        ArrayGrid arrayGrid = ArrayGrid.createWith2DDoubleArray(GridDef.createGlobal(0.05));

        assertEquals(3600, arrayGrid.getHeight());
        assertEquals(7200, arrayGrid.getWidth());
        assertEquals(1.0, arrayGrid.getScaling(), 0.0);
        assertEquals(0.0, arrayGrid.getOffset(), 0.0);

        assertEquals(0.0, arrayGrid.getSampleDouble(0, 0), 0.0);
        assertEquals(0.0, arrayGrid.getSampleDouble(0, 3559), 0.0);
        try {
            arrayGrid.getSampleDouble(0, 3600);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
        assertEquals(0.0, arrayGrid.getSampleDouble(7199, 3559), 0.0);
        try {
            arrayGrid.getSampleDouble(7200, 3559);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }
    }
}
