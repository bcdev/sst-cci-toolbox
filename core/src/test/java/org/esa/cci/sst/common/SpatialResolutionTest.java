/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.common;

import org.esa.cci.sst.grid.GridDef;
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
