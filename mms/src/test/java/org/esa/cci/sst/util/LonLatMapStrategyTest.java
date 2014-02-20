package org.esa.cci.sst.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LonLatMapStrategyTest {

    private LonLatMapStrategy strategy;

    @Before
    public void setUp() {
        strategy = new LonLatMapStrategy(800, 400);
    }

    @Test
    public void testMap() {
        SamplingPoint samplingPoint = new SamplingPoint(-34.4, 12.66, 77745387L, 0.3);
        PlotPoint point = strategy.map(samplingPoint);
        assertNotNull(point);
        assertEquals(323, point.getX());
        assertEquals(171, point.getY());

        samplingPoint = new SamplingPoint(66.886, -42.66, 77745387L, 0.3);
        point = strategy.map(samplingPoint);
        assertNotNull(point);
        assertEquals(548, point.getX());
        assertEquals(294, point.getY());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testInterfaceImplemented() {
         assertTrue(strategy instanceof MapStrategy);
    }
}
