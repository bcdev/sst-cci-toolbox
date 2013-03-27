package org.esa.cci.sst.regrid;/*
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

import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WriterHelperTest {

    @Test
    public void testCreateLatData() throws Exception {
        final GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] latData = WriterHelper.createLatData(gridDef);
        assertEquals(18, latData.length);

        final float[] expected = new float[]{
                85.0f,
                75.0f,
                65.0f,
                55.0f,
                45.0f,
                35.0f,
                25.0f,
                15.0f,
                5.0f,
                -5.0f,
                -15.0f,
                -25.0f,
                -35.0f,
                -45.0f,
                -55.0f,
                -65.0f,
                -75.0f,
                -85.0f
        };
        assertTrue(Arrays.equals(expected, latData));
    }

    @Test
    public void testCreateLonData() throws Exception {
        GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] lonData = WriterHelper.createLonData(gridDef);
        assertEquals(36, lonData.length);

        final float[] expected = new float[]{
                -175.0f,
                -165.0f,
                -155.0f,
                -145.0f,
                -135.0f,
                -125.0f,
                -115.0f,
                -105.0f,
                -95.0f,
                -85.0f,
                -75.0f,
                -65.0f,
                -55.0f,
                -45.0f,
                -35.0f,
                -25.0f,
                -15.0f,
                -5.0f,
                5.0f,
                15.0f,
                25.0f,
                35.0f,
                45.0f,
                55.0f,
                65.0f,
                75.0f,
                85.0f,
                95.0f,
                105.0f,
                115.0f,
                125.0f,
                135.0f,
                145.0f,
                155.0f,
                165.0f,
                175.0f
        };
        assertTrue(Arrays.equals(expected, lonData));
    }

}
