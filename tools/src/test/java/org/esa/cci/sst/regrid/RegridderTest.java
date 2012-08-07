package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.TestL3ProductMaker;
import org.junit.Test;
import ucar.ma2.Array;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Bettina Scholze
 *         Date: 26.07.12 10:37
 */
public class RegridderTest {

    @Test
    public void testInitialiseTargetGrids() throws Exception {
        SpatialResolution targetResolution = SpatialResolution.DEGREE_10_00;
        Regridder regridder = new Regridder(null, String.valueOf(targetResolution.getValue()), new File(""), "0", SstDepth.sea_surface_temperature);
        Map<String, ArrayGrid> sourceArrayGrids = TestL3ProductMaker.fetchL3UProductForTest();

        //execution
        Map<String, ArrayGrid> targetGrids = regridder.initialiseTargetGridsFrom(sourceArrayGrids);

        assertEquals(12, targetGrids.size());
        String[] targetVariables = {"uncorrelated_uncertainty", "sea_surface_temperature", "large_scale_correlated_uncertainty",
                "sses_standard_deviation", "quality_level", "synoptically_correlated_uncertainty", "sst_dtime", "l2p_flags",
                "wind_speed", "adjustment_uncertainty", "sses_bias", "sea_surface_temperature_depth"};

        assertArrayEquals(targetVariables, targetGrids.keySet().toArray(new String[targetGrids.size()]));

        for (int i = 0; i < targetGrids.size(); i++) {
            String variable = targetVariables[i];
            ArrayGrid arrayGrid2Check = targetGrids.get(variable);
            verifyGridForVariable(targetResolution, arrayGrid2Check, variable);
        }
    }

    private static void verifyGridForVariable(SpatialResolution resolution, ArrayGrid arrayGrid2Check, String variable) {
        assertEquals(variable, arrayGrid2Check.getVariable());
        int timeDim = 1;
        assertTrue(variable, GridDef.createGlobal(resolution.getValue(), timeDim).equals(arrayGrid2Check.getGridDef()));

        Number fillValue = arrayGrid2Check.getFillValue();
        assertTrue("fill value type", fillValue == null || fillValue instanceof Integer ||
                fillValue instanceof Short || fillValue instanceof Byte);
        assertNotNull(variable, arrayGrid2Check.getScaling());
        assertNotNull(variable, arrayGrid2Check.getScaling());

        assertEquals(variable, 18, arrayGrid2Check.getHeight());
        assertEquals(variable, 36, arrayGrid2Check.getWidth());

        Array array = arrayGrid2Check.getArray();
        Object javaArray = array.copyToNDJavaArray();

        String[] expectedShortValues = {"uncorrelated_uncertainty", "large_scale_correlated_uncertainty", "sea_surface_temperature",
                "synoptically_correlated_uncertainty", "adjustment_uncertainty", "l2p_flags", "sea_surface_temperature_depth"};
        String[] expectedByteValues = {"quality_level", "sses_standard_deviation", "wind_speed", "sses_bias"};
        String[] expectedIntValues = {"sst_dtime"};

        if (javaArray instanceof short[][][]) {
            short[][][] data = (short[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(18, data[0].length);
            assertEquals(36, data[0][0].length);
            assertTrue("Not expected to be of type short", Arrays.toString(expectedShortValues).contains(variable));
            //test that no data
            short[] values = data[0][0];
            for (short value : values) {
                assertEquals(0, value);
            }
        } else if (javaArray instanceof byte[][][]) {
            byte[][][] data = (byte[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(18, data[0].length);
            assertEquals(36, data[0][0].length);
            assertTrue("Not expected to be of type byte", Arrays.toString(expectedByteValues).contains(variable));
            //test that no data
            byte[] values = data[0][0];
            for (byte value : values) {
                assertEquals(0, value);
            }
        } else if (javaArray instanceof int[][][]) {
            int[][][] data = (int[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(18, data[0].length);
            assertEquals(36, data[0][0].length);
            assertTrue("Not expected to be of type int", Arrays.toString(expectedIntValues).contains(variable));
            //test that no data
            int[] values = data[0][0];
            for (int value : values) {
                assertEquals(0, value);
            }
        }
    }
}
