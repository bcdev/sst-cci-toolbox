/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.util;

import org.esa.cci.sst.util.PgUtil;
import org.junit.Test;
import org.postgis.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PgUtilTest {

    @Test
    public void testOrientationOfCounterclockwiseGeoBoundary() {
        final List<Point> geoBoundary = createCounterclockwiseGeoBoundary();
        assertFalse(PgUtil.isClockwise(geoBoundary));
    }

    @Test
    public void testOrientationOfClockwiseGeoBoundary() {
        final List<Point> geoBoundary = createCounterclockwiseGeoBoundary();
        Collections.reverse(geoBoundary);
        assertTrue(PgUtil.isClockwise(geoBoundary));
    }

    private static List<Point> createCounterclockwiseGeoBoundary() {
        final List<Point> geoBoundary = new ArrayList<Point>(5);
        geoBoundary.add(new Point(0.0, 1.0));
        geoBoundary.add(new Point(0.0, 0.0));
        geoBoundary.add(new Point(1.0, 0.0));
        geoBoundary.add(new Point(1.0, 1.0));
        geoBoundary.add(geoBoundary.get(0));
        return geoBoundary;
    }
}
