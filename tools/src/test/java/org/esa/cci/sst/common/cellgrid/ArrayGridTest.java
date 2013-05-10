package org.esa.cci.sst.common.cellgrid;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static java.lang.Double.NaN;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class ArrayGridTest {

    private static final double[] ARRAY_DATA = new double[]{
            999, 0.2, 999, 0.4, 0.1, 0.2, 999, 0.4,
            999, 0.3, 999, 999, 0.2, 0.3, 0.4, 999,
            0.3, 999, 0.1, 0.2, 0.3, 0.4, 999, 999,
            999, 0.1, 0.2, 0.3, 0.4, 0.1, 999, 999,
    };
    private GridDef gridDef;
    private ArrayGrid arrayGrid;


    @Before
    public void setUp() throws Exception {
        gridDef = GridDef.createGlobal(8, 4);

        final int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        final Array array = Array.factory(DataType.DOUBLE, shape, ARRAY_DATA);
        arrayGrid = new ArrayGrid(gridDef, array, 999, 1.0, 0.0);
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
    public void testGetSample() throws Exception {
        ArrayGrid arrayGrid = ArrayGrid.create(GridDef.createGlobal(0.05), (double[]) null);

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
