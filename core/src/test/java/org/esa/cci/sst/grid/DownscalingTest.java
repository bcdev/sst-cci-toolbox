package org.esa.cci.sst.grid;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DownscalingTest {

    private Grid sourceGrid;

    @Before
    public void setUp() throws Exception {
        final double[] data = new double[]{
                999, 0.2, 999, 0.4, 0.1, 0.2, 999, 0.4,
                999, 0.3, 999, 999, 0.2, 0.3, 0.4, 999,
                0.3, 999, 0.1, 0.2, 0.3, 0.4, 999, 999,
                999, 0.1, 0.2, 0.3, 0.4, 0.1, 999, 999,
        };

        final int w = 8;
        final int h = 4;
        final GridDef gridDef = GridDef.createGlobal(w, h);
        final Array array = Array.factory(DataType.DOUBLE, new int[]{h, w}, data);

        sourceGrid = new ArrayGrid(gridDef, array, 999.0, 1.0, 0.0);
    }

    @Test
    public void testDownscaling() throws Exception {
        final Grid scaledGrid = Downscaling.create(sourceGrid, 2);
        final GridDef scaledGridDef = scaledGrid.getGridDef();

        final GridDef sourceGridDef = sourceGrid.getGridDef();
        final int sourceW = sourceGridDef.getWidth();
        final int sourceH = sourceGridDef.getHeight();

        assertEquals(sourceW / 2, scaledGridDef.getWidth());
        assertEquals(sourceH / 2, scaledGridDef.getHeight());
        assertEquals(sourceGridDef.getEasting(), scaledGridDef.getEasting(), 1e-10);
        assertEquals(sourceGridDef.getNorthing(), scaledGridDef.getNorthing(), 1e-10);
        assertEquals(sourceGridDef.getResolutionX() * 2, scaledGridDef.getResolutionX(), 1e-10);
        assertEquals(sourceGridDef.getResolutionY() * 2, scaledGridDef.getResolutionY(), 1e-10);

        assertEquals(0.25, scaledGrid.getSampleDouble(0, 0), 1.0e-10);
        assertEquals(0.40, scaledGrid.getSampleDouble(1, 0), 1.0e-10);
        assertEquals(0.20, scaledGrid.getSampleDouble(2, 0), 1.0e-10);
        assertEquals(0.40, scaledGrid.getSampleDouble(3, 0), 1.0e-10);
        //
        assertEquals(0.20, scaledGrid.getSampleDouble(0, 1), 1.0e-10);
        assertEquals(0.20, scaledGrid.getSampleDouble(1, 1), 1.0e-10);
        assertEquals(0.30, scaledGrid.getSampleDouble(2, 1), 1.0e-10);
        assertTrue(Double.isNaN(scaledGrid.getSampleDouble(3, 1)));
    }

    @Test
    public void testGetBoolean() throws Exception {
        final Grid scaledGrid = Downscaling.create(sourceGrid, 2);
        try {
            scaledGrid.getSampleBoolean(0, 0);
            fail();
        } catch (RuntimeException expected) {
        }
    }
}

