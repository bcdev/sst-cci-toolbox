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
