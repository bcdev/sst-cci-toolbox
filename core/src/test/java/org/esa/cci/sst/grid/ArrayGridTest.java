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
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ArrayGridTest {

    private static final double[] ARRAY_DATA = new double[]{
            999, 0.2, 999, 0.4, 0.1, 0.2, 999, 0.4,
            999, 0.3, 999, 999, 0.2, 0.3, 0.4, 999,
            0.3, 999, 0.1, 0.2, 0.3, 0.4, 999, 999,
            999, 0.1, 0.2, 0.3, 0.4, 0.1, 999, 999,
    };
    private ArrayGrid arrayGrid;

    @Before
    public void setUp() throws Exception {
        final GridDef gridDef = GridDef.createGlobal(8, 4);

        final int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        final Array array = Array.factory(DataType.DOUBLE, shape, ARRAY_DATA);
        arrayGrid = new ArrayGrid(gridDef, array, 999, 1.0, 0.0);
    }

    @Test
    public void testFillValue() throws Exception {

        double[] expected = new double[]{
                NaN, 0.2, NaN, 0.4, 0.1, 0.2, NaN, 0.4,
                NaN, 0.3, NaN, NaN, 0.2, 0.3, 0.4, NaN,
                0.3, NaN, 0.1, 0.2, 0.3, 0.4, NaN, NaN,
                NaN, 0.1, 0.2, 0.3, 0.4, 0.1, NaN, NaN,
        };

        double[] actual = new double[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = arrayGrid.getSampleDouble(i % 8, i / 8);
        }
        assertArrayEquals(expected, actual, 1e-10);
    }

    @Test
    public void testGetSample() throws Exception {
        final ArrayGrid arrayGrid = ArrayGrid.create(GridDef.createGlobal(0.05), (double[]) null);

        assertEquals(3600, arrayGrid.getHeight());
        assertEquals(7200, arrayGrid.getWidth());
        assertEquals(1.0, arrayGrid.getScaling(), 0.0);
        assertEquals(0.0, arrayGrid.getOffset(), 0.0);

        assertEquals(0.0, arrayGrid.getSampleDouble(0, 0), 0.0);
        assertEquals(0.0, arrayGrid.getSampleDouble(0, 3559), 0.0);
    }

    @Test
    public void testSetSample() {
        final ArrayGrid arrayGrid = ArrayGrid.create(GridDef.createGlobal(0.05), (double[]) null);

        assertEquals(0.0, arrayGrid.getSampleDouble(19, 108), 1e-8);

        arrayGrid.setSample(19, 108, 22.8);
        assertEquals(22.8, arrayGrid.getSampleDouble(19, 108), 1e-8);
    }

    @Test
    public void testSetSample_outOfBounds() {
        final ArrayGrid arrayGrid = ArrayGrid.create(GridDef.createGlobal(0.5), (double[]) null);

        assertEquals(720, arrayGrid.getWidth());
        assertEquals(360, arrayGrid.getHeight());

        try {
            arrayGrid.setSample(721, 5, 1.9);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        try {
            arrayGrid.setSample(-1, 5, 1.9);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        try {
            arrayGrid.setSample(19, 361, 1.9);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        try {
            arrayGrid.setSample(19, -1, 1.9);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
    }

    @Test
    public void testGetSample_outOfBounds() throws Exception {
        final ArrayGrid arrayGrid = ArrayGrid.create(GridDef.createGlobal(0.05), (double[]) null);

        try {
            arrayGrid.getSampleDouble(0, 3600);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }

        assertEquals(0.0, arrayGrid.getSampleDouble(7199, 3559), 0.0);
        try {
            arrayGrid.getSampleDouble(7200, 3559);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetSampleBoolean() throws Exception {
        final boolean[] data = {false, true, true, true, true, false, true, true};
        final Array array = Array.factory(data);

        final Grid grid = new ArrayGrid(GridDef.createGlobal(90), array, 19, 1.0, 0.0);

        assertTrue(grid.getSampleBoolean(1, 0));
        assertTrue(grid.getSampleBoolean(3, 1));

        assertFalse(grid.getSampleBoolean(0, 0));
        assertFalse(grid.getSampleBoolean(1, 1));
    }

    @Test
    public void testGetSampleBoolean_outOfBounds() throws Exception {
        final boolean[] data = {false, true, true, true, true, false, true, true};
        final Array array = Array.factory(data);

        final Grid grid = new ArrayGrid(GridDef.createGlobal(90), array, 19, 1.0, 0.0);

        try {
            grid.getSampleBoolean(4, 1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }

        try {
            grid.getSampleBoolean(0, 3);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetSampleInt() throws Exception {
        final int[] data = {0, 1, 2, 3, 4, 5, 6, 7};
        final Array array = Array.factory(data);

        final Grid grid = new ArrayGrid(GridDef.createGlobal(90), array, 19, 1.0, 0.0);

        assertEquals(1, grid.getSampleInt(1, 0));
        assertEquals(5, grid.getSampleInt(1, 1));
    }

    @Test
    public void testGetSampleInt_outOfBounds() throws Exception {
        final int[] data = {0, 1, 2, 3, 4, 5, 6, 7};
        final Array array = Array.factory(data);

        final Grid grid = new ArrayGrid(GridDef.createGlobal(90), array, 19, 1.0, 0.0);

        try {
            grid.getSampleInt(-1, 1);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }

        try {
            grid.getSampleInt(0, 3);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (Exception e) {
        }
    }
}
