package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.cellgrid.GridDef;
import org.junit.Test;
import ucar.ma2.Array;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 14:13
 */
public class SpatialResolutionTest {

    @Test
    public void testSpatialResolution() throws Exception {
        assertEquals(24, SpatialResolution.values().length);
    }

    @Test
    public void testGetValuesAsString() throws Exception {
        String expected = "[0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.75, 0.8, 1.0, 1.2, 1.25, 2.0, 2.25, 2.4, 2.5, 3.0, 3.75, 4.0, 4.5, 5.0, 10.0]";
        assertEquals(expected, SpatialResolution.getValuesAsString());
    }

    @Test
    public void testGetDefaultAsString() throws Exception {
        assertEquals("5.0", SpatialResolution.getDefaultValueAsString());
    }

    @Test
    public void testGetAssociatedGridDef() throws Exception {
        GridDef associatedGridDef = SpatialResolution.DEGREE_0_10.getAssociatedGridDef();
        assertEquals(0.10, associatedGridDef.getResolutionX(), 0.001);
        assertEquals(0.10, associatedGridDef.getResolutionY(), 0.001);
    }

    @Test
    public void testConvertShape() throws Exception {
        //1)
        double sourceResolution = SpatialResolution.DEGREE_0_10.getValue();
        SpatialResolution targetResolution = SpatialResolution.DEGREE_0_50;
        int[] sourceShape = {2, 1800, 3600};

        int[] resultShape = SpatialResolution.convertShape(targetResolution, sourceShape, GridDef.createGlobal(sourceResolution));

        assertArrayEquals(new int[]{2, 360, 720}, resultShape);

        //2)
        sourceResolution = SpatialResolution.DEGREE_2_50.getValue();
        targetResolution = SpatialResolution.DEGREE_0_10;
        int[] sourceShape_2 = {72, 2, 144, 0};

        int[] resultShape_2 = SpatialResolution.convertShape(targetResolution, sourceShape_2, GridDef.createGlobal(sourceResolution));
        assertArrayEquals(new int[]{1800, 2, 3600, 0}, resultShape_2);
    }

    @Test
    public void testName() throws Exception {
        SpatialResolution resolution = SpatialResolution.DEGREE_10_00;
        GridDef gridDef = resolution.getAssociatedGridDef();

        Map<String,Array> baseArrays = resolution.calculateBaseArrays();

        assertEquals(4, baseArrays.size());
        final Array lat = baseArrays.get("lat");
        final Array lon = baseArrays.get("lon");
        final Array latBnds = baseArrays.get("lat_bnds");
        final Array lonBnds = baseArrays.get("lon_bnds");

        assertNotNull(lat);
        assertNotNull(lon);
        assertNotNull(latBnds);
        assertNotNull(lonBnds);

        assertTrue(lat.copyTo1DJavaArray() instanceof float[]);
        assertTrue(latBnds.copyTo1DJavaArray() instanceof float[]);
        assertTrue(lon.copyTo1DJavaArray() instanceof float[]);
        assertTrue(lonBnds.copyTo1DJavaArray() instanceof float[]);

        float[] latData = (float[]) lat.copyTo1DJavaArray();
        float[] latBndsData = (float[]) latBnds.copyTo1DJavaArray();
        float[] lonData = (float[]) lon.copyTo1DJavaArray();
        float[] lonBndsData = (float[]) lonBnds.copyTo1DJavaArray();

        assertEquals(gridDef.getHeight(), latData.length);
        assertEquals("[85.0, 75.0, 65.0, 55.0, 45.0, 35.0, 25.0, 15.0, 5.0, " +
                "-5.0, -15.0, -25.0, -35.0, -45.0, -55.0, -65.0, -75.0, -85.0]", Arrays.toString(latData));
        assertEquals(2 * gridDef.getHeight(), latBndsData.length);
        assertEquals("[90.0, 80.0, 80.0, 70.0, 70.0, 60.0, 60.0, 50.0, " +
                "50.0, 40.0, 40.0, 30.0, 30.0, 20.0, 20.0, 10.0, 10.0, 0.0, " +
                "0.0, -10.0, -10.0, -20.0, -20.0, -30.0, -30.0, -40.0, -40.0, -50.0, " +
                "-50.0, -60.0, -60.0, -70.0, -70.0, -80.0, -80.0, -90.0]", Arrays.toString(latBndsData));
        assertEquals(gridDef.getWidth(), lonData.length);
        assertEquals("[175.0, 165.0, 155.0, 145.0, 135.0, 125.0, 115.0, 105.0, " +
                "95.0, 85.0, 75.0, 65.0, 55.0, 45.0, 35.0, 25.0, 15.0, 5.0, " +
                "-5.0, -15.0, -25.0, -35.0, -45.0, -55.0, -65.0, -75.0, -85.0, -95.0, " +
                "-105.0, -115.0, -125.0, -135.0, -145.0, -155.0, -165.0, -175.0]", Arrays.toString(lonData));
        assertEquals(2 * gridDef.getWidth(), lonBndsData.length);
        assertEquals("[180.0, 170.0, 170.0, 160.0, 160.0, 150.0, 150.0, 140.0, 140.0, 130.0, 130.0, 120.0, 120.0, 110.0, 110.0, 100.0, " +
                "100.0, 90.0, 90.0, 80.0, 80.0, 70.0, 70.0, 60.0, 60.0, 50.0, 50.0, 40.0, 40.0, 30.0, 30.0, 20.0, 20.0, 10.0, 10.0, 0.0, " +
                "0.0, -10.0, -10.0, -20.0, -20.0, -30.0, -30.0, -40.0, -40.0, -50.0, -50.0, -60.0, -60.0, -70.0, -70.0, -80.0, -80.0, -90.0, " +
                "-90.0, -100.0, -100.0, -110.0, -110.0, -120.0, -120.0, -130.0, -130.0, -140.0, -140.0, -150.0, -150.0, -160.0, -160.0, -170.0, " +
                "-170.0, -180.0]", Arrays.toString(lonBndsData));
    }

    @Test
    public void testCreateArrayGridWith1DArray_lat_1() throws Exception {
        GridDef gridDef = SpatialResolution.DEGREE_10_00.getAssociatedGridDef(1);

        float[] data = SpatialResolution.createArrayGridWith1DArray(gridDef, 18, 90f);

        assertEquals(18, data.length);
        final String expected = "[85.0, 75.0, 65.0, 55.0, 45.0, 35.0, 25.0, 15.0, 5.0," +
                " -5.0, -15.0, -25.0, -35.0, -45.0, -55.0, -65.0, -75.0, -85.0]";
        assertEquals(expected, Arrays.toString(data));
    }

    @Test
    public void testCreateArrayGridWith1DArray_lat_2() throws Exception {
        GridDef gridDef = SpatialResolution.DEGREE_5_00.getAssociatedGridDef(1);

        float[] data = SpatialResolution.createArrayGridWith1DArray(gridDef, 36, 90f);

        assertEquals(36, data.length);
        final String expected = "[87.5, 82.5, 77.5, 72.5, 67.5, 62.5, 57.5, 52.5, 47.5, 42.5, 37.5, 32.5, 27.5, 22.5, 17.5, 12.5, 7.5, 2.5, " +
                "-2.5, -7.5, -12.5, -17.5, -22.5, -27.5, -32.5, -37.5, -42.5, -47.5, -52.5, -57.5, -62.5, -67.5, -72.5, -77.5, -82.5, -87.5]";
        assertEquals(expected, Arrays.toString(data));
    }

    @Test
    public void testCreateArrayGridWith1DArray_lon() throws Exception {
        GridDef gridDef = SpatialResolution.DEGREE_10_00.getAssociatedGridDef(1);

        float[] data = SpatialResolution.createArrayGridWith1DArray(gridDef, 36, 180f);

        assertEquals(36, data.length);
        float[] expected = new float[]{175.0f, 165.0f, 155.0f, 145.0f, 135.0f, 125.0f, 115.0f, 105.0f,
                95.0f, 85.0f, 75.0f, 65.0f, 55.0f, 45.0f, 35.0f, 25.0f, 15.0f, 5.0f,
                -5.0f, -15.0f, -25.0f, -35.0f, -45.0f, -55.0f, -65.0f, -75.0f, -85.0f, -95.0f,
                -105.0f, -115.0f, -125.0f, -135.0f, -145.0f, -155.0f, -165.0f, -175.0f};
        assertEquals(Arrays.toString(expected), Arrays.toString(data));
    }
}
