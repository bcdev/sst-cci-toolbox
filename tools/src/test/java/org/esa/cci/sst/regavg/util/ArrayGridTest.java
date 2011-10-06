package org.esa.cci.sst.regavg.util;

import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static java.lang.Double.*;
import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ArrayGridTest {

    @Test
    public void testScaleDown() throws Exception {
        GridDef gridDef = GridDef.createGlobalGrid(8, 4);
        int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        double[] data = new double[]{
                999, 0.2, 999, 0.4, 0.1, 0.2, 999, 0.4,
                999, 0.3, 999, 999, 0.2, 0.3, 0.4, 999,
                0.3, 999, 0.1, 0.2, 0.3, 0.4, 999, 999,
                999, 0.1, 0.2, 0.3, 0.4, 0.1, 999, 999,
        };

        Array array = Array.factory(DataType.DOUBLE, shape, data);
        ArrayGrid arrayGrid = new ArrayGrid(gridDef, 1.0, 0.0, 999, array);

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
        assertArrayEquals(expected, (double[]) arrayGrid2.getArray().getStorage(), 1e-10);
    }

    @Test
    public void testFillValue() throws Exception {
        GridDef gridDef = GridDef.createGlobalGrid(8, 4);
        int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        double[] data = new double[]{
                0.1, 0.2, 0.3, 0.4, 0.1, 999, 0.3, 0.4,
                0.2, 999, 0.4, 0.1, 0.2, 0.3, 0.4, 0.1,
                0.3, 0.4, 0.1, 999, 0.3, 0.4, 0.1, 0.2,
                0.4, 0.1, 0.2, 0.3, 0.4, 0.1, 0.2, 999,
        };

        Array array = Array.factory(DataType.DOUBLE, shape, data);
        ArrayGrid arrayGrid = new ArrayGrid(gridDef, 1.0, 0.0, 999.0, array);
        double[] expected = new double[]{
                0.1, 0.2, 0.3, 0.4, 0.1, NaN, 0.3, 0.4,
                0.2, NaN, 0.4, 0.1, 0.2, 0.3, 0.4, 0.1,
                0.3, 0.4, 0.1, NaN, 0.3, 0.4, 0.1, 0.2,
                0.4, 0.1, 0.2, 0.3, 0.4, 0.1, 0.2, NaN,
        };

        double[] retrievedData = new double[expected.length];
        for (int i = 0; i < retrievedData.length; i++) {
            retrievedData[i] = arrayGrid.getSampleDouble(i % 8, i / 8);
        }
        assertArrayEquals(expected, retrievedData, 1e-10);
    }
}
