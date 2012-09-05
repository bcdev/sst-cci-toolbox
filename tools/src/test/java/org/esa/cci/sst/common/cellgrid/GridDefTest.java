package org.esa.cci.sst.common.cellgrid;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class GridDefTest {
    GridDef gridDef90;
    GridDef gridDef5;
    GridDef gridDef05;

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

        assertEquals(135.0, gridDef90.getCenterLon(-1), 1e-10);
        assertEquals(-135.0, gridDef90.getCenterLon(0), 1e-10);
        assertEquals(-45.0, gridDef90.getCenterLon(1), 1e-10);
        assertEquals(45.0, gridDef90.getCenterLon(2), 1e-10);
        assertEquals(135.0, gridDef90.getCenterLon(3), 1e-10);
        assertEquals(-135.0, gridDef90.getCenterLon(4), 1e-10);

        assertEquals(90.0, gridDef90.getCenterLat(-1), 1e-10);
        assertEquals(45.0, gridDef90.getCenterLat(0), 1e-10);
        assertEquals(-45.0, gridDef90.getCenterLat(1), 1e-10);
        assertEquals(-90.0, gridDef90.getCenterLat(3), 1e-10);
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
    public void testGetLonLatRectangle() throws Exception {

        assertEquals(new Rectangle2D.Double(-180.0, 89.95, 0.05, 0.05),
                gridDef05.getLonLatRectangle(0, 0));

        assertEquals(new Rectangle2D.Double(0.0, 0.0, 0.05, 0.05),
                gridDef05.getLonLatRectangle(gridDef05.getWidth() / 2, gridDef05.getHeight() / 2 - 1));
    }

    @Test
    public void testEquals() throws Exception {

        assertTrue(GridDef.createGlobal(0.05).equals(
                GridDef.createGlobal(0.05)));

        assertFalse(GridDef.createGlobal(0.04).equals(
                GridDef.createGlobal(0.05)));

        assertTrue(GridDef.createGlobalGrid(3600, 1800).equals(
                GridDef.createGlobalGrid(3600, 1800)));

        assertFalse(GridDef.createGlobalGrid(3600, 1800).equals(
                GridDef.createGlobalGrid(3600, 1801)));

        assertTrue(new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03).equals(
                new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03)));

        assertFalse(new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03).equals(
                new GridDef(3, 6, 9.3, -8.4, 0.02, 0.03)));

        GridDef expectedGridDef_1 = new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03);
        expectedGridDef_1.setTime(345452445);
        assertFalse(expectedGridDef_1.equals(new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03)));

        GridDef expectedGridDef_2 = new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03);
        int time_2 = 345452445;
        expectedGridDef_2.setTime(time_2);
        GridDef gridDef_2 = new GridDef(3, 6, 9.3, -8.4, 0.01, 0.03);
        gridDef_2.setTime(time_2);
        assertTrue(expectedGridDef_2.equals(gridDef_2));

    }
}
