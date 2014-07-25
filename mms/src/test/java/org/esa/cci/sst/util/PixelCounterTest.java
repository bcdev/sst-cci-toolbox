package org.esa.cci.sst.util;/*
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

import org.junit.Test;
import ucar.ma2.Array;

import static org.junit.Assert.assertEquals;

public class PixelCounterTest {

    @Test
    public void testCountPixels_all_pixels_valid() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter();

        final Array array = Array.factory(new byte[][]{
                {0b0000, 0b0000, 0b0000},
                {0b0000, 0b0000, 0b0000},
                {0b0000, 0b0000, 0b0000}
        });

        assertEquals(0, pixelCounter.count(array));
    }

    @Test
    public void testCountPixels_all_pixels_invalid() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter();

        final Array mask = Array.factory(new byte[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, 0b1001}
        });

        assertEquals(9, pixelCounter.count(mask));
    }

    @Test
    public void testCountPixels_with_null_argument() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter();

        assertEquals(0, pixelCounter.count(null));
    }
}
