package org.esa.cci.sst.grid;

import org.esa.cci.sst.common.Coverage;
import org.esa.cci.sst.common.SpatialResolution;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class RegionMaskTest {

    @Test
    public void testGetSampleBoolean_CenterFieldSet() throws Exception {
        final char[] data = createData(73, 36);

        set(data, 36, 18, '1');

        final RegionMask mask = RegionMask.create("test", new String(data));

        assertEquals(false, mask.getSampleBoolean(35, 17));
        assertEquals(false, mask.getSampleBoolean(36, 17));
        assertEquals(false, mask.getSampleBoolean(37, 17));

        assertEquals(false, mask.getSampleBoolean(35, 18));
        assertEquals(true, mask.getSampleBoolean(36, 18));
        assertEquals(false, mask.getSampleBoolean(37, 18));

        assertEquals(false, mask.getSampleBoolean(35, 19));
        assertEquals(false, mask.getSampleBoolean(36, 19));
        assertEquals(false, mask.getSampleBoolean(37, 19));
    }

    @Test
    public void testGetSampleInt_CenterFieldSet() throws Exception {
        final char[] data = createData(73, 36);

        set(data, 36, 18, '1');

        final RegionMask mask = RegionMask.create("test", new String(data));

        assertEquals(0, mask.getSampleInt(35, 17));
        assertEquals(1, mask.getSampleInt(36, 18));
        assertEquals(0, mask.getSampleInt(35, 19));
    }

    @Test
    public void testGetSampleDouble_CenterFieldSet() throws Exception {
        final char[] data = createData(73, 36);

        set(data, 36, 18, '1');

        final RegionMask mask = RegionMask.create("test", new String(data));

        assertEquals(0.0, mask.getSampleDouble(35, 17), 1e-8);
        assertEquals(1.0, mask.getSampleDouble(36, 18), 1e-8);
        assertEquals(0.0, mask.getSampleDouble(35, 19), 1e-8);
    }

    @Test
    public void testCoverage() throws Exception {
        Assert.assertEquals(Coverage.GLOBE, RegionMask.create("X", -180, +90, +180, -90).getCoverage());
        assertEquals(Coverage.N_HEMISPHERE, RegionMask.create("X", -180, +90, +180, 0).getCoverage());
        assertEquals(Coverage.S_HEMISPHERE, RegionMask.create("X", -180, 0, +180, -90).getCoverage());
        assertEquals(Coverage.OTHER, RegionMask.create("X", 0, 10, 10, 0).getCoverage());
        assertEquals(Coverage.OTHER, RegionMask.create("X", -180, 90, 180, -85).getCoverage());
    }

    @Test
    public void testSetSpatialResolution() {
        RegionMask.setSpatialResolution(SpatialResolution.DEGREE_2_00);

        try {
            final RegionMask mask = RegionMask.create("X", 0, 10, 10, 0);

            assertEquals(180, mask.getWidth());
            assertEquals(90, mask.getHeight());

            final GridDef gridDef = mask.getGridDef();
            assertNotNull(gridDef);
            assertEquals(2.0, gridDef.getResolutionX(), 1e-8);
            assertEquals(2.0, gridDef.getResolutionY(), 1e-8);
        } finally {
            // reset to default
            RegionMask.setSpatialResolution(SpatialResolution.DEGREE_5_00);
        }
    }

    @Test
    public void testCreate_invalidLineWidth() {
        final char[] data = createData(56, 36);

        try {
            RegionMask.create("test", new String(data));
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testCreate_invalidHeight() {
        final char[] data = createData(73, 11);

        try {
            RegionMask.create("test", new String(data));
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testCreate_invalidData() {
        final char[] data = createData(73, 36);

        set(data, 11, 19, 'I');

        try {
            RegionMask.create("test", new String(data));
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testCreate_southBelowNorth() {
        try {
            RegionMask.create("X", 0, 10, 10, 20);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCreate_intersectsAntiMeridian() {
        final RegionMask mask = RegionMask.create("X", 170, 10, -170, 0);

        assertTrue(mask.getSampleBoolean(70, 16));
        assertFalse(mask.getSampleBoolean(10, 10));
    }

    @Test
    public void testCreate_emptyCoverage() throws ParseException {
        final char[] data = createData(73, 36);


        final RegionMask mask = RegionMask.create("test", new String(data));
        assertEquals(Coverage.EMPTY, mask.getCoverage());
    }

    @Test
    public void testGetName() {
        final RegionMask mask = RegionMask.create("the_name", 0, 10, 10, 0);

        assertEquals("the_name", mask.getName());
    }

    private void set(char[] data, int x, int y, char c) {
        data[73 * y + x] = c;
    }

    private char[] createData(int width, int height) {
        final char[] data = new char[width * height];
        for (int i = 0; i < data.length; i++) {
            if ((i + 1) % width == 0) {
                data[i] = '\n';
            } else {
                data[i] = '0';
            }
        }
        return data;
    }
}
