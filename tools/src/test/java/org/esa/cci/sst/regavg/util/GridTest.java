package org.esa.cci.sst.regavg.util;

import org.esa.cci.sst.util.Grid;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class GridTest {
     Grid globalGrid ;

    @Before
    public void setUp() throws Exception {
        globalGrid = Grid.createGlobalGrid(90.0);
        assertEquals(4, globalGrid.getWidth());
        assertEquals(2, globalGrid.getHeight());
    }

    @Test
    public void testGridXY() throws Exception {

        assertEquals(0, globalGrid.getGridX(-180.0, false));
        assertEquals(1, globalGrid.getGridX(-90.0, false));
        assertEquals(2, globalGrid.getGridX(0.0, false));
        assertEquals(3, globalGrid.getGridX(90.0, false));
        assertEquals(4, globalGrid.getGridX(180.0, false));

        assertEquals(0, globalGrid.getGridY(90.0, false));
        assertEquals(1, globalGrid.getGridY(0.0, false));
        assertEquals(2, globalGrid.getGridY(-90.0, false));

        try {
            globalGrid.getGridX(-180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            globalGrid.getGridX(180.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            globalGrid.getGridY(-90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            globalGrid.getGridY(90.1, false);
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testCenterLonLat() throws Exception {

        assertEquals(-225.0, globalGrid.getCenterLon(-1), 1e-10);
        assertEquals(-135.0, globalGrid.getCenterLon(0), 1e-10);
        assertEquals(-45.0, globalGrid.getCenterLon(1), 1e-10);
        assertEquals(45.0, globalGrid.getCenterLon(2), 1e-10);
        assertEquals(135.0, globalGrid.getCenterLon(3), 1e-10);
        assertEquals(225.0, globalGrid.getCenterLon(4), 1e-10);

        assertEquals(90.0, globalGrid.getCenterLat(-1), 1e-10);
        assertEquals(45.0, globalGrid.getCenterLat(0), 1e-10);
        assertEquals(-45.0, globalGrid.getCenterLat(1), 1e-10);
        assertEquals(-90.0, globalGrid.getCenterLat(3), 1e-10);
    }
}
