package org.esa.cci.sst.tools.overlap;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PolarOrbitingPolygonTest {

    @Test
    public void testNormLongitude() {
        assertEquals(0.0, PolarOrbitingPolygon.normLongitude(0.0), 1e-8);
        assertEquals(-23.0, PolarOrbitingPolygon.normLongitude(-23.0), 1e-8);
        assertEquals(-179.9, PolarOrbitingPolygon.normLongitude(-179.9), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normLongitude(-180.1), 1e-8);
        assertEquals(0.1, PolarOrbitingPolygon.normLongitude(-359.9), 1e-8);
        assertEquals(-0.1, PolarOrbitingPolygon.normLongitude(-360.1), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normLongitude(-540.1), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normLongitude(179.9), 1e-8);
        assertEquals(-179.9, PolarOrbitingPolygon.normLongitude(180.1), 1e-8);
    }
}
