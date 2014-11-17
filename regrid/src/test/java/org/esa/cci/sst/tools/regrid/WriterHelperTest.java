package org.esa.cci.sst.tools.regrid;/*
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
import org.esa.cci.sst.grid.GridDef;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class WriterHelperTest {

    private final float[] EXPECTED_LAT = new float[]{
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
    private final float[] EXPECTED_LON = new float[]{
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

    @Test
    public void testCreateLatData() throws Exception {
        final GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] latData = WriterHelper.createLatData(gridDef);
        assertArrayEquals(EXPECTED_LAT, latData, 1e-8f);
    }

    @Test
    public void testCreateLonData() throws Exception {
        final GridDef gridDef = SpatialResolution.DEGREE_10_00.getGridDef();

        final float[] lonData = WriterHelper.createLonData(gridDef);
        assertArrayEquals(EXPECTED_LON, lonData, 1e-8f);
    }
}
