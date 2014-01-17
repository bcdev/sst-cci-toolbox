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
    public void testCountPixels_bit_one_set() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter(0b0001);

        final Array array = Array.factory(new int[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, 0b1001}
        });

        assertEquals(5, pixelCounter.count(array));
    }

    @Test
    public void testCountPixels_bit_two_set() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter(0b0010);

        final Array array = Array.factory(new int[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, 0b1001}
        });

        assertEquals(4, pixelCounter.count(array));
    }

    @Test
    public void testCountPixels_bit_one_or_two_set() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter(0b0011);

        final Array array = Array.factory(new int[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, 0b1001}
        });

        assertEquals(7, pixelCounter.count(array));
    }

    @Test
    public void testCountPixels_bit_three_set() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter(0b0100, Integer.MIN_VALUE);

        final Array array = Array.factory(new int[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, 0b1001}
        });

        assertEquals(4, pixelCounter.count(array));
    }

    @Test
    public void testCountPixels_bit_three_set_or_fill_value() throws Exception {
        final PixelCounter pixelCounter = new PixelCounter(0b0100, Integer.MIN_VALUE);

        final Array array = Array.factory(new int[][]{
                {0b0001, 0b0010, 0b0011},
                {0b0100, 0b0101, 0b0110},
                {0b0111, 0b1000, Integer.MIN_VALUE}
        });

        assertEquals(5, pixelCounter.count(array));
    }
}
