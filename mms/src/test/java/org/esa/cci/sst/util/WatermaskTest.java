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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WatermaskTest {

    @Test
    public void testWatermask() throws Exception {

        final Watermask watermask = new Watermask();

        assertTrue(watermask.isWater(0.0, 0.0));
        assertFalse(watermask.isWater(20.0, 0.0));
    }

    // for testing performance
    public static void main(String[] args) {
        final Watermask watermask = new Watermask();
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(4);

        final long t0 = System.currentTimeMillis();
        System.out.println("Start time = " + new Date(t0));

        for (int i = 0; i < 10000000; i++) {
            final double[] sample = sequenceGenerator.nextVector();
            final double x = sample[0];
            final double y = sample[1];

            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;

            watermask.isWater(lon, lat);
        }
        final long t1 = System.currentTimeMillis();
        System.out.println("Stop time  = " + new Date(t1));
        final long elapsedTime = (t1 - t0) / 1000;
        System.out.println("Time elapsed = " + elapsedTime + " seconds");
    }

}