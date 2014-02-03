package org.esa.cci.sst.tools.overlap;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PolarOrbitingPolygonTest {

    @Test
    public void testNormLongitude() {
        assertEquals(0.0, PolarOrbitingPolygon.normalizeLongitude(0.0), 1e-8);
        assertEquals(-23.0, PolarOrbitingPolygon.normalizeLongitude(-23.0), 1e-8);
        assertEquals(-179.9, PolarOrbitingPolygon.normalizeLongitude(-179.9), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normalizeLongitude(-180.1), 1e-8);
        assertEquals(0.1, PolarOrbitingPolygon.normalizeLongitude(-359.9), 1e-8);
        assertEquals(-0.1, PolarOrbitingPolygon.normalizeLongitude(-360.1), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normalizeLongitude(-540.1), 1e-8);

        assertEquals(179.9, PolarOrbitingPolygon.normalizeLongitude(179.9), 1e-8);
        assertEquals(-179.9, PolarOrbitingPolygon.normalizeLongitude(180.1), 1e-8);
    }

    @Test
    public void testIsEdgeCrossingEquator() {
         assertTrue(PolarOrbitingPolygon.isEdgeCrossingEquator(-1.0, 1.0));
         assertTrue(PolarOrbitingPolygon.isEdgeCrossingEquator(1.0, -1.0));

         assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(1.0, 1.0));
         assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(-1.0, -1.0));
    }
}
