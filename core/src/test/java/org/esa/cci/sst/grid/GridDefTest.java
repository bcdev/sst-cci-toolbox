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

package org.esa.cci.sst.grid;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class GridDefTest {

    private GridDef gridDef90;
    private GridDef gridDef5;
    private GridDef gridDef05;

    @Before
    public void setUp() throws Exception {
        gridDef90 = GridDef.createGlobal(90.0);
        assertEquals(4, gridDef90.getWidth());
        assertEquals(2, gridDef90.getHeight());

        gridDef5 = GridDef.createGlobal(5.0);
        assertEquals(72, gridDef5.getWidth());
        assertEquals(36, gridDef5.getHeight());

        gridDef05 = GridDef.createGlobal(0.05);
        assertEquals(7200, gridDef05.getWidth());
        assertEquals(3600, gridDef05.getHeight());
    }

    @Test
    public void testWrapX() throws Exception {
        final GridDef gridDef = GridDef.createGlobal(8, 4);

        assertEquals(0, gridDef.wrapX(0));
        assertEquals(7, gridDef.wrapX(7));

        assertEquals(7, gridDef.wrapX(-1));
        assertEquals(6, gridDef.wrapX(-2));
        assertEquals(5, gridDef.wrapX(-3));
        assertEquals(4, gridDef.wrapX(-4));
        assertEquals(3, gridDef.wrapX(-5));
        assertEquals(2, gridDef.wrapX(-6));
        assertEquals(1, gridDef.wrapX(-7));
        assertEquals(0, gridDef.wrapX(-8));
        assertEquals(7, gridDef.wrapX(-9));

        assertEquals(0, gridDef.wrapX(8));
        assertEquals(1, gridDef.wrapX(9));
        assertEquals(2, gridDef.wrapX(10));
        assertEquals(3, gridDef.wrapX(11));
        assertEquals(4, gridDef.wrapX(12));
        assertEquals(5, gridDef.wrapX(13));
        assertEquals(6, gridDef.wrapX(14));
        assertEquals(7, gridDef.wrapX(15));
        assertEquals(0, gridDef.wrapX(16));
    }

    @Test
    public void testGridXY() throws Exception {
        assertEquals(0, gridDef90.getGridX(-180.0, false));
        assertEquals(1, gridDef90.getGridX(-90.0, false));
        assertEquals(2, gridDef90.getGridX(0.0, false));
        assertEquals(3, gridDef90.getGridX(90.0, false));
        assertEquals(4, gridDef90.getGridX(180.0, false));

        assertEquals(0, gridDef90.getGridX(-180.0, true));
        assertEquals(1, gridDef90.getGridX(-90.0, true));
        assertEquals(2, gridDef90.getGridX(0.0, true));
        assertEquals(3, gridDef90.getGridX(90.0, true));
        assertEquals(3, gridDef90.getGridX(180.0, true));

        assertEquals(0, gridDef90.getGridY(90.0, false));
        assertEquals(1, gridDef90.getGridY(0.0, false));
        assertEquals(2, gridDef90.getGridY(-90.0, false));

        assertEquals(0, gridDef90.getGridY(90.0, true));
        assertEquals(1, gridDef90.getGridY(0.0, true));
        assertEquals(1, gridDef90.getGridY(-90.0, true));

        try {
            gridDef90.getGridX(-180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            gridDef90.getGridX(180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            gridDef90.getGridY(-90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            gridDef90.getGridY(90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testCenterLonLat() throws Exception {
        try {
            assertEquals(135.0, gridDef90.getCenterLon(-1), 1e-10);
            fail();
        } catch (Exception expected) {
        }
        assertEquals(-135.0, gridDef90.getCenterLon(0), 1e-10);
        assertEquals(-45.0, gridDef90.getCenterLon(1), 1e-10);
        assertEquals(45.0, gridDef90.getCenterLon(2), 1e-10);
        assertEquals(135.0, gridDef90.getCenterLon(3), 1e-10);
        try {
            assertEquals(-135.0, gridDef90.getCenterLon(4), 1e-10);
            fail();
        } catch (Exception expected) {
        }

        try {
            assertEquals(90.0, gridDef90.getCenterLat(-1), 1e-10);
            fail();
        } catch (Exception expected) {
        }
        assertEquals(45.0, gridDef90.getCenterLat(0), 1e-10);
        assertEquals(-45.0, gridDef90.getCenterLat(1), 1e-10);
        try {
            assertEquals(-90.0, gridDef90.getCenterLat(3), 1e-10);
            fail();
        } catch (Exception expected) {
        }
    }

    @Test
    public void testGetGridRectangle1() throws Exception {
        assertEquals(new Rectangle(0, 0, gridDef5.getWidth(), gridDef5.getHeight()),
                gridDef5.getGridRectangle(-180.0, -90.0, 180.0, 90.0));

        assertEquals(new Rectangle(0, gridDef5.getHeight() / 2, gridDef5.getWidth() / 2, gridDef5.getHeight() / 2),
                gridDef5.getGridRectangle(-180.0, -90.0, 0.0, 0.0));

        assertEquals(new Rectangle(gridDef5.getWidth() / 2, gridDef5.getHeight() / 2 - 1, 1, 1),
                gridDef5.getGridRectangle(0.0, 0.0, 5.0, 5.0));

        assertEquals(new Rectangle(gridDef90.getWidth() / 2, 0, 2, 1),
                gridDef90.getGridRectangle(0.0, 0.0, 180.0, 90.0));

        assertEquals(new Rectangle(0, 0, 1, 2),
                gridDef90.getGridRectangle(-180.0, -90.0, -90.0, 90.0));
    }

    @Test
    public void testGetGridRectangle2() throws Exception {
        assertEquals(new Rectangle(0, 0, gridDef5.getWidth(), gridDef5.getHeight()),
                gridDef5.getGridRectangle(new Rectangle2D.Double(-180.0, -90.0, 360.0, 180.0)));

        assertEquals(new Rectangle(0, gridDef5.getHeight() / 2, gridDef5.getWidth() / 2, gridDef5.getHeight() / 2),
                gridDef5.getGridRectangle(new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0)));

        assertEquals(new Rectangle(gridDef5.getWidth() / 2, gridDef5.getHeight() / 2 - 1, 1, 1),
                gridDef5.getGridRectangle(new Rectangle2D.Double(0.0, 0.0, 5.0, 5.0)));

        assertEquals(new Rectangle(gridDef90.getWidth() / 2, gridDef90.getHeight() / 2, 2, 1),
                gridDef90.getGridRectangle(new Rectangle2D.Double(0.0, -90.0, 180.0, 90.0)));
    }

    @Test
    public void testGetGridRectangle3() throws Exception {
        assertEquals(new Rectangle(0, 0, 100, 100), gridDef05.getGridRectangle(0, 0, gridDef5));
        assertEquals(new Rectangle(100, 0, 100, 100), gridDef05.getGridRectangle(1, 0, gridDef5));
        assertEquals(new Rectangle(0, 100, 100, 100), gridDef05.getGridRectangle(0, 1, gridDef5));
        assertEquals(new Rectangle(100, 100, 100, 100), gridDef05.getGridRectangle(1, 1, gridDef5));
    }

    @Test
    public void testGetGridRectangle_resolutionTooCoarse() {
        try {
            gridDef5.getGridRectangle(0, 0, gridDef05);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetLonLatRectangle() throws Exception {
        assertEquals(new Rectangle2D.Double(-180.0, 89.95, 0.05, 0.05),
                gridDef05.getLonLatRectangle(0, 0));

        assertEquals(new Rectangle2D.Double(0.0, 0.0, 0.05, 0.05),
                gridDef05.getLonLatRectangle(gridDef05.getWidth() / 2, gridDef05.getHeight() / 2 - 1));
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(GridDef.createGlobal(0.05).equals(GridDef.createGlobal(0.05)));
        assertFalse(GridDef.createGlobal(0.04).equals(GridDef.createGlobal(0.05)));

        assertTrue(GridDef.createGlobal(3600, 1800).equals(GridDef.createGlobal(3600, 1800)));
        assertFalse(GridDef.createGlobal(3600, 1800).equals(GridDef.createGlobal(3600, 1801)));
    }

    @Test
    public void testCreateRaster() {
        final GridDef raster = GridDef.createRaster(100, 50);
        assertNotNull(raster);

        assertEquals(100, raster.getWidth());
        assertEquals(50, raster.getHeight());

        assertEquals(0.0, raster.getEasting(), 1e-8);
        assertEquals(0.0, raster.getNorthing(), 1e-8);
        assertEquals(0.0, raster.getResolutionX(), 1e-8);
        assertEquals(0.0, raster.getResolutionY(), 1e-8);
    }

    @Test
    public void testGetResolution() {
        final GridDef gridDef = GridDef.createGlobal(60, 30);

        assertEquals(6.0, gridDef.getResolution(), 1e-8);
    }

    @Test
    public void testGetResolution_throwsIfResolutionDiffersInAxes() {
        final GridDef gridDef = GridDef.createGlobal(60, 19);

        try {
            gridDef.getResolution();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetDiagonal() {
        assertEquals(10007.543398010286, gridDef90.getDiagonal(0, 0), 1e-8);
        assertEquals(10007.543398010286, gridDef90.getDiagonal(2, 0), 1e-8);
        assertEquals(10007.543398010286, gridDef90.getDiagonal(0, 2), 1e-8);

        assertEquals(555.9746332227928, gridDef5.getDiagonal(0, 0), 1e-8);
        assertEquals(555.9746332227928, gridDef5.getDiagonal(10, 0), 1e-8);
        assertEquals(742.9651956574585, gridDef5.getDiagonal(0, 12), 1e-8);
    }

    @Test
    public void testHashCode() {
        int hashCode = gridDef05.hashCode();
        assertEquals(-480847887, hashCode);

        hashCode = gridDef5.hashCode();
        assertEquals(1939683365, hashCode);

        hashCode = gridDef90.hashCode();
        assertEquals(885044675, hashCode);
    }
}
