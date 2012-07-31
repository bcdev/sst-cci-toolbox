package org.esa.cci.sst.util;

import org.esa.cci.sst.regrid.SpatialResolution;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Bettina Scholze
 *         Date: 26.07.12 09:50
 */
public class NcUtilsTest {

    @Test
    public void testReadL3Grids() throws Exception {
        NetcdfFile netcdfFile = TestL3ProductMaker.readL3GridsSetup();
        SpatialResolution resolution = SpatialResolution.DEGREE_0_05;

        //execution
        Map<String, ArrayGrid> arrayGrids = NcUtils.readL3Grids(netcdfFile, resolution.getAssociatedGridDef());

        //verification
        assertEquals(12, arrayGrids.size());

        String[] variables = {"uncorrelated_uncertainty", "large_scale_correlated_uncertainty", "sea_surface_temperature",
                "synoptically_correlated_uncertainty", "quality_level", "sses_standard_deviation", "adjustment_uncertainty",
                "wind_speed", "l2p_flags", "sst_dtime", "sea_surface_temperature_depth", "sses_bias"};
        assertArrayEquals(variables, arrayGrids.keySet().toArray(new String[arrayGrids.size()]));

        for (int i = 0; i < arrayGrids.size(); i++) {
            String variable = variables[i];
            ArrayGrid arrayGrid2Check = arrayGrids.get(variable);
            verifyGridForVariable(resolution, arrayGrid2Check, variable);
        }
    }

    private static void verifyGridForVariable(SpatialResolution resolution, ArrayGrid arrayGrid2Check, String variable) {
        assertEquals(variable, arrayGrid2Check.getVariable());
        assertTrue(variable, GridDef.createGlobal(resolution.getValue()).equals(arrayGrid2Check.getGridDef()));

        Number fillValue = arrayGrid2Check.getFillValue();
        assertTrue("fill value type", fillValue == null || fillValue instanceof Integer ||
                fillValue instanceof Short || fillValue instanceof Byte);
        assertNotNull(variable, arrayGrid2Check.getScaling());

        assertEquals(variable, 3600, arrayGrid2Check.getHeight());
        assertEquals(variable, 7200, arrayGrid2Check.getWidth());

        //check the data
        boolean isSST = "sea_surface_temperature".equals(variable);
        Array array = arrayGrid2Check.getArray();
        Object javaArray = array.copyToNDJavaArray();

        String[] expectedShortValues = {"uncorrelated_uncertainty", "large_scale_correlated_uncertainty", "sea_surface_temperature",
                "synoptically_correlated_uncertainty", "adjustment_uncertainty", "l2p_flags", "sea_surface_temperature_depth"};
        String[] expectedByteValues = {"quality_level", "sses_standard_deviation", "wind_speed", "sses_bias"};
        String[] expectedIntValues = {"sst_dtime"};

        if (javaArray instanceof short[][][]) {
            short[][][] data = (short[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(3600, data[0].length);
            assertEquals(7200, data[0][0].length);
            assertTrue("Not expected to be of type short", Arrays.toString(expectedShortValues).contains(variable));
            //test that no data
            if (!isSST) {
                short[] values = data[0][0];
                for (short value : values) {
                    assertTrue(Short.MIN_VALUE == value || Short.MIN_VALUE + 1 == value);
                }
            }
        } else if (javaArray instanceof byte[][][]) {
            byte[][][] data = (byte[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(3600, data[0].length);
            assertEquals(7200, data[0][0].length);
            assertTrue("Not expected to be of type byte", Arrays.toString(expectedByteValues).contains(variable));
            //test that no data
            byte[] values = data[0][0];
            for (byte value : values) {
                assertTrue(Byte.MIN_VALUE == value || Byte.MIN_VALUE + 1 == value);
            }
        } else if (javaArray instanceof int[][][]) {
            int[][][] data = (int[][][]) javaArray;
            assertEquals(1, data.length);
            assertEquals(3600, data[0].length);
            assertEquals(7200, data[0][0].length);
            assertTrue("Not expected to be of type int", Arrays.toString(expectedIntValues).contains(variable));
            //test that no data
            int[] values = data[0][0];
            for (int value : values) {
                assertEquals(0, value);
            }
        }

        if (isSST) {
            short[][][] sstData = (short[][][]) javaArray;
            short[] sstValues = sstData[0][0];

            for (int i = 0; i < sstValues.length; i++) {
                int value = i % 2 == 0 ? 2000 : 1000;
                assertEquals(value, sstValues[i]);
            }
        }
    }

    @Test
    public void testGetGridResolution() throws Exception {
        double gridResolution = NcUtils.getGridResolution(new TestNetcdfFile());
        assertEquals(5.0, gridResolution);
    }

    @Test
    public void testGetGridResolution_2() throws Exception {
        try {
            NcUtils.getGridResolution(new TestNetcdfFile_2());
            fail("IOException expected");
        } catch (IOException expected) {
            assertEquals("Product is not L3 or L4, dimension lat or lon is missing or not equally scaled.", expected.getMessage());
        }
    }

    private class TestNetcdfFile extends NetcdfFile {
        @Override
        public List<Dimension> getDimensions() {
            ArrayList<Dimension> dimList = new ArrayList<Dimension>();
            dimList.add(new Dimension("bnds", 2));
            dimList.add(new Dimension("lat", 36));
            dimList.add(new Dimension("lon", 72));
            return dimList;
        }
    }

    private class TestNetcdfFile_2 extends NetcdfFile {
        @Override
        public List<Dimension> getDimensions() {
            ArrayList<Dimension> dimList = new ArrayList<Dimension>();
            dimList.add(new Dimension("lat", 123));
            dimList.add(new Dimension("bnds", 2));
            dimList.add(new Dimension("lon", 72));
            return dimList;
        }
    }


}
