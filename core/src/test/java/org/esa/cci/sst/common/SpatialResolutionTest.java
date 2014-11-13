package org.esa.cci.sst.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SpatialResolutionTest {

    @Test
    public void testSpatialResolution() throws Exception {
        assertEquals(24, SpatialResolution.values().length);
    }

    @Test
    public void testGetValuesAsString() throws Exception {
        String expected = "[0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.75, 0.8, 1.0, 1.2, 1.25, 2.0, 2.25, 2.4, 2.5, 3.0, 3.75, 4.0, 4.5, 5.0, 10.0]";
        assertEquals(expected, SpatialResolution.getAllResolutionsAsString());
    }

    @Test
    public void testGetDefaultAsString() throws Exception {
        assertEquals("5.0", SpatialResolution.getDefaultResolutionAsString());
    }

    @Test
    public void testGetAssociatedGridDef() throws Exception {
        GridDef associatedGridDef = SpatialResolution.DEGREE_0_10.getGridDef();
        assertEquals(0.10, associatedGridDef.getResolutionX(), 0.001);
        assertEquals(0.10, associatedGridDef.getResolutionY(), 0.001);
    }

    @Test
    public void testGetSpatialResolution_fromString() throws Exception {
        assertEquals(SpatialResolution.DEGREE_0_60, SpatialResolution.getSpatialResolution("0.6"));
        assertEquals(SpatialResolution.DEGREE_1_25, SpatialResolution.getSpatialResolution("1.25"));
    }

    @Test
    public void testGetSpatialResolution_fromString_invalidArgument() throws Exception {
        try {
            SpatialResolution.getSpatialResolution("11.7765");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetSpatialResolution_fromDouble() throws Exception {
        assertEquals(SpatialResolution.DEGREE_0_25, SpatialResolution.getSpatialResolution(0.25));
        assertEquals(SpatialResolution.DEGREE_2_50, SpatialResolution.getSpatialResolution(2.5));
    }

    @Test
    public void testGetSpatialResolution_fromDouble_invalidArgument() throws Exception {
        try {
            SpatialResolution.getSpatialResolution(-2008.067);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
