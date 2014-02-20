package org.esa.cci.sst.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlotPointTest {

    @Test
    public void testConstructionAndGetter() {
        final PlotPoint point = new PlotPoint(8, 56);

        assertEquals(8, point.getX());
        assertEquals(56, point.getY());
    }
}
