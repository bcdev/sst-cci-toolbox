package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.grid.GridDef;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class WriterHelperTest {

    private final float[] EXPECTED_LAT = new float[]{
            85.0f,
            75.0f,
            65.0f,
            55.0f,
            45.0f,
            35.0f,
            25.0f,
            15.0f,
            5.0f,
            -5.0f,
            -15.0f,
            -25.0f,
            -35.0f,
            -45.0f,
            -55.0f,
            -65.0f,
            -75.0f,
            -85.0f
    };
    private final float[] EXPECTED_LON = new float[]{
            -175.0f,
            -165.0f,
            -155.0f,
            -145.0f,
            -135.0f,
            -125.0f,
            -115.0f,
            -105.0f,
            -95.0f,
            -85.0f,
            -75.0f,
            -65.0f,
            -55.0f,
            -45.0f,
            -35.0f,
            -25.0f,
            -15.0f,
            -5.0f,
            5.0f,
            15.0f,
            25.0f,
            35.0f,
            45.0f,
            55.0f,
            65.0f,
            75.0f,
            85.0f,
            95.0f,
            105.0f,
            115.0f,
            125.0f,
            135.0f,
            145.0f,
            155.0f,
            165.0f,
            175.0f
    };

    @Test
    public void testCreateLatData() throws Exception {
        final GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] latData = WriterHelper.createLatData(gridDef);
        assertArrayEquals(EXPECTED_LAT, latData, 1e-8f);
    }

    @Test
    public void testCreateLonData() throws Exception {
        final GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] lonData = WriterHelper.createLonData(gridDef);
        assertArrayEquals(EXPECTED_LON, lonData, 1e-8f);
    }

    @Test
    public void testCreateLatBoundsData() {
        final GridDef gridDef = SpatialResolution.DEGREE_5_00.getGridDef();

        final float[][] latBoundsData = WriterHelper.createLatBoundsData(gridDef);
        assertEquals(36, latBoundsData.length);
        assertEquals(2, latBoundsData[0].length);

        assertEquals(85.0, latBoundsData[0][0], 1e-8);
        assertEquals(90.0, latBoundsData[0][1], 1e-8);

        assertEquals(0.0, latBoundsData[17][0], 1e-8);
        assertEquals(5.0, latBoundsData[17][1], 1e-8);

        assertEquals(-90.0, latBoundsData[35][0], 1e-8);
        assertEquals(-85.0, latBoundsData[35][1], 1e-8);
    }

    @Test
    public void testCreateLonBoundsData() {
        final GridDef gridDef = SpatialResolution.DEGREE_5_00.getGridDef();

        final float[][] lonBoundsData = WriterHelper.createLonBoundsData(gridDef);
        assertEquals(72, lonBoundsData.length);
        assertEquals(2, lonBoundsData[0].length);

        assertEquals(-180.0, lonBoundsData[0][0], 1e-8);
        assertEquals(-175.0, lonBoundsData[0][1], 1e-8);

        assertEquals(-5.0, lonBoundsData[35][0], 1e-8);
        assertEquals(0.0, lonBoundsData[35][1], 1e-8);

        assertEquals(175.0, lonBoundsData[71][0], 1e-8);
        assertEquals(180.0, lonBoundsData[71][1], 1e-8);
    }
}
