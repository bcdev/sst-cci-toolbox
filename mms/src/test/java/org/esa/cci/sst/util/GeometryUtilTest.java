package org.esa.cci.sst.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeometryUtilTest {

    @Test
    public void testNormalizeLongitude() {
        assertEquals(0.0, GeometryUtil.normalizeLongitude(0.0), 1e-8);
        assertEquals(-23.0, GeometryUtil.normalizeLongitude(-23.0), 1e-8);
        assertEquals(-179.9, GeometryUtil.normalizeLongitude(-179.9), 1e-8);

        assertEquals(179.9, GeometryUtil.normalizeLongitude(-180.1), 1e-8);
        assertEquals(0.1, GeometryUtil.normalizeLongitude(-359.9), 1e-8);
        assertEquals(-0.1, GeometryUtil.normalizeLongitude(-360.1), 1e-8);

        assertEquals(179.9, GeometryUtil.normalizeLongitude(-540.1), 1e-8);

        assertEquals(179.9, GeometryUtil.normalizeLongitude(179.9), 1e-8);
        assertEquals(-179.9, GeometryUtil.normalizeLongitude(180.1), 1e-8);
    }
}
