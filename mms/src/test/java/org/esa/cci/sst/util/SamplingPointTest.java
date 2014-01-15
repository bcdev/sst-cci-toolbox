package org.esa.cci.sst.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SamplingPointTest {

    @Test
    public void testConstructWith_x_y() {
        final SamplingPoint samplingPoint = new SamplingPoint(16, 345);

        assertEquals(16, samplingPoint.getX());
        assertEquals(345, samplingPoint.getY());
    }
}
