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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InterpolationTest {

    private static final double D00 = 0.25;
    private static final double D01 = 0.40;
    private static final double D02 = 0.20;
    private static final double D03 = 0.40;
    private static final double D10 = 0.20;
    private static final double D11 = 0.20;
    private static final double D12 = 0.30;
    private static final double D13 = 0.10;
    private Grid sourceGrid;

    @Before
    public void setUp() throws Exception {
        final double[] data = new double[]{
                D00, D01, D02, D03,
                D10, D11, D12, D13,
        };

        final int w = 4;
        final int h = 2;
        final GridDef gridDef = GridDef.createGlobal(w, h);
        final Array array = Array.factory(DataType.DOUBLE, new int[]{h, w}, data);

        sourceGrid = new ArrayGrid(gridDef, array, null, 1.0, 0.0);
    }

    @Test
    public void testInterpolation() throws Exception {
        final GridDef sourceGridDef = sourceGrid.getGridDef();
        final int sourceW = sourceGridDef.getWidth();
        final int sourceH = sourceGridDef.getHeight();

        final GridDef targetGridDef = GridDef.createGlobal(sourceW * 2, sourceH * 2);
        final Grid targetGrid = Interpolation.create(sourceGrid, targetGridDef);

        assertEquals(sourceW * 2, targetGridDef.getWidth());
        assertEquals(sourceH * 2, targetGridDef.getHeight());
        assertEquals(sourceGridDef.getEasting(), targetGridDef.getEasting(), 1e-10);
        assertEquals(sourceGridDef.getNorthing(), targetGridDef.getNorthing(), 1e-10);
        assertEquals(sourceGridDef.getResolutionX() / 2.0, targetGridDef.getResolutionX(), 1e-10);
        assertEquals(sourceGridDef.getResolutionY() / 2.0, targetGridDef.getResolutionY(), 1e-10);

        final double e00 = 0.25 * D03 + 0.75 * D00;
        final double e01 = 0.75 * D00 + 0.25 * D01;
        final double e02 = 0.25 * D00 + 0.75 * D01;
        final double e03 = 0.75 * D01 + 0.25 * D02;
        final double e04 = 0.25 * D01 + 0.75 * D02;
        final double e05 = 0.75 * D02 + 0.25 * D03;
        final double e06 = 0.25 * D02 + 0.75 * D03;
        final double e07 = 0.75 * D03 + 0.25 * D00;

        assertEquals(e00, targetGrid.getSampleDouble(0, 0), 1.0e-10);
        assertEquals(e01, targetGrid.getSampleDouble(1, 0), 1.0e-10);
        assertEquals(e02, targetGrid.getSampleDouble(2, 0), 1.0e-10);
        assertEquals(e03, targetGrid.getSampleDouble(3, 0), 1.0e-10);
        assertEquals(e04, targetGrid.getSampleDouble(4, 0), 1.0e-10);
        assertEquals(e05, targetGrid.getSampleDouble(5, 0), 1.0e-10);
        assertEquals(e06, targetGrid.getSampleDouble(6, 0), 1.0e-10);
        assertEquals(e07, targetGrid.getSampleDouble(7, 0), 1.0e-10);

        final double e30 = 0.25 * D13 + 0.75 * D10;
        final double e31 = 0.75 * D10 + 0.25 * D11;
        final double e32 = 0.25 * D10 + 0.75 * D11;
        final double e33 = 0.75 * D11 + 0.25 * D12;
        final double e34 = 0.25 * D11 + 0.75 * D12;
        final double e35 = 0.75 * D12 + 0.25 * D13;
        final double e36 = 0.25 * D12 + 0.75 * D13;
        final double e37 = 0.75 * D13 + 0.25 * D10;

        assertEquals(e30, targetGrid.getSampleDouble(0, 3), 1.0e-10);
        assertEquals(e31, targetGrid.getSampleDouble(1, 3), 1.0e-10);
        assertEquals(e32, targetGrid.getSampleDouble(2, 3), 1.0e-10);
        assertEquals(e33, targetGrid.getSampleDouble(3, 3), 1.0e-10);
        assertEquals(e34, targetGrid.getSampleDouble(4, 3), 1.0e-10);
        assertEquals(e35, targetGrid.getSampleDouble(5, 3), 1.0e-10);
        assertEquals(e36, targetGrid.getSampleDouble(6, 3), 1.0e-10);
        assertEquals(e37, targetGrid.getSampleDouble(7, 3), 1.0e-10);

        assertEquals(0.75 * e00 + 0.25 * e30, targetGrid.getSampleDouble(0, 1), 1.0e-10);
        assertEquals(0.75 * e01 + 0.25 * e31, targetGrid.getSampleDouble(1, 1), 1.0e-10);
        assertEquals(0.75 * e02 + 0.25 * e32, targetGrid.getSampleDouble(2, 1), 1.0e-10);
        assertEquals(0.75 * e03 + 0.25 * e33, targetGrid.getSampleDouble(3, 1), 1.0e-10);
        assertEquals(0.75 * e04 + 0.25 * e34, targetGrid.getSampleDouble(4, 1), 1.0e-10);
        assertEquals(0.75 * e05 + 0.25 * e35, targetGrid.getSampleDouble(5, 1), 1.0e-10);
        assertEquals(0.75 * e06 + 0.25 * e36, targetGrid.getSampleDouble(6, 1), 1.0e-10);
        assertEquals(0.75 * e07 + 0.25 * e37, targetGrid.getSampleDouble(7, 1), 1.0e-10);

        assertEquals(0.25 * e00 + 0.75 * e30, targetGrid.getSampleDouble(0, 2), 1.0e-10);
        assertEquals(0.25 * e01 + 0.75 * e31, targetGrid.getSampleDouble(1, 2), 1.0e-10);
        assertEquals(0.25 * e02 + 0.75 * e32, targetGrid.getSampleDouble(2, 2), 1.0e-10);
        assertEquals(0.25 * e03 + 0.75 * e33, targetGrid.getSampleDouble(3, 2), 1.0e-10);
        assertEquals(0.25 * e04 + 0.75 * e34, targetGrid.getSampleDouble(4, 2), 1.0e-10);
        assertEquals(0.25 * e05 + 0.75 * e35, targetGrid.getSampleDouble(5, 2), 1.0e-10);
        assertEquals(0.25 * e06 + 0.75 * e36, targetGrid.getSampleDouble(6, 2), 1.0e-10);
        assertEquals(0.25 * e07 + 0.75 * e37, targetGrid.getSampleDouble(7, 2), 1.0e-10);
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
