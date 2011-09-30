package org.esa.cci.sst.regavg.util;

import org.esa.cci.sst.util.Grid;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class GridTest {
    Grid grid90;
    Grid grid5;
    Grid grid05;

    @Before
    public void setUp() throws Exception {
        grid90 = Grid.createGlobalGrid(90.0);
        assertEquals(4, grid90.getWidth());
        assertEquals(2, grid90.getHeight());

        grid5 = Grid.createGlobalGrid(5.0);
        assertEquals(72, grid5.getWidth());
        assertEquals(36, grid5.getHeight());

        grid05 = Grid.createGlobalGrid(0.05);
        assertEquals(7200, grid05.getWidth());
        assertEquals(3600, grid05.getHeight());
    }

    @Test
    public void testGridXY() throws Exception {

        assertEquals(0, grid90.getGridX(-180.0, false));
        assertEquals(1, grid90.getGridX(-90.0, false));
        assertEquals(2, grid90.getGridX(0.0, false));
        assertEquals(3, grid90.getGridX(90.0, false));
        assertEquals(4, grid90.getGridX(180.0, false));

        assertEquals(0, grid90.getGridY(90.0, false));
        assertEquals(1, grid90.getGridY(0.0, false));
        assertEquals(2, grid90.getGridY(-90.0, false));

        try {
            grid90.getGridX(-180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            grid90.getGridX(180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            grid90.getGridY(-90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            grid90.getGridY(90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testCenterLonLat() throws Exception {

        assertEquals(135.0, grid90.getCenterLon(-1), 1e-10);
        assertEquals(-135.0, grid90.getCenterLon(0), 1e-10);
        assertEquals(-45.0, grid90.getCenterLon(1), 1e-10);
        assertEquals(45.0, grid90.getCenterLon(2), 1e-10);
        assertEquals(135.0, grid90.getCenterLon(3), 1e-10);
        assertEquals(-135.0, grid90.getCenterLon(4), 1e-10);

        assertEquals(90.0, grid90.getCenterLat(-1), 1e-10);
        assertEquals(45.0, grid90.getCenterLat(0), 1e-10);
        assertEquals(-45.0, grid90.getCenterLat(1), 1e-10);
        assertEquals(-90.0, grid90.getCenterLat(3), 1e-10);
    }

    @Test
    public void testGetGridRectangle1() throws Exception {

        assertEquals(new Rectangle(0, 0, grid5.getWidth(), grid5.getHeight()),
                     grid5.getGridRectangle(-180.0, -90.0, 180.0, 90.0));

        assertEquals(new Rectangle(0, grid5.getHeight() / 2, grid5.getWidth() / 2, grid5.getHeight() / 2),
                     grid5.getGridRectangle(-180.0, -90.0, 0.0, 0.0));

        assertEquals(new Rectangle(grid5.getWidth() / 2, grid5.getHeight() / 2 - 1, 1, 1),
                     grid5.getGridRectangle(0.0, 0.0, 5.0, 5.0));
    }

    @Test
    public void testGetGridRectangle2() throws Exception {

        assertEquals(new Rectangle(0, 0, grid5.getWidth(), grid5.getHeight()),
                     grid5.getGridRectangle(new Rectangle2D.Double(-180.0, -90.0, 360.0, 180.0)));

        assertEquals(new Rectangle(0, grid5.getHeight() / 2, grid5.getWidth() / 2, grid5.getHeight() / 2),
                     grid5.getGridRectangle(new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0)));

        assertEquals(new Rectangle(grid5.getWidth() / 2, grid5.getHeight() / 2 - 1, 1, 1),
                     grid5.getGridRectangle(new Rectangle2D.Double(0.0, 0.0, 5.0, 5.0)));
    }

    @Test
    public void testGetLonLatRectangle() throws Exception {

        assertEquals(new Rectangle2D.Double(-180.0, 89.95, 0.05, 0.05),
                     grid05.getLonLatRectangle(0, 0));

        assertEquals(new Rectangle2D.Double(0.0, 0.0, 0.05, 0.05),
                     grid05.getLonLatRectangle(grid05.getWidth() / 2, grid05.getHeight() / 2 - 1));
    }
}
