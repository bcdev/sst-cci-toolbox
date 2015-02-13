package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverlapCalculatorTest {

    @Test
    public void testIntersect_3x3() {
        final OverlapCalculator ic = new OverlapCalculator(3, 3);

        final SamplingPoint p0 = new SamplingPoint(5, 7);
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(5, 7)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(7, 7)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(3, 7)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(5, 9)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(5, 5)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(7, 9)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(3, 5)));

        assertFalse(ic.areOverlapping(p0, new SamplingPoint(2, 5)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(8, 5)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(5, 10)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(5, 4)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(10, 6)));
    }

    @Test
    public void testIntersect_5x7() {
        final OverlapCalculator ic = new OverlapCalculator(5, 7);

        final SamplingPoint p0 = new SamplingPoint(100, 100);
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(104, 100)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(96, 100)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(100, 106)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(100, 94)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(104, 94)));
        assertTrue(ic.areOverlapping(p0, new SamplingPoint(104, 106)));

        assertFalse(ic.areOverlapping(p0, new SamplingPoint(105, 100)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(95, 100)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(100, 107)));
        assertFalse(ic.areOverlapping(p0, new SamplingPoint(100, 93)));
    }
}
