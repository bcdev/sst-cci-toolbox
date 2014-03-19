package org.esa.cci.sst.util;

import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    @Test
    public void testCreatePointGeometry() {
        final PGgeometry point = GeometryUtil.createPointGeometry(34.0, 11.6);
        assertNotNull(point);
        final Geometry pointGeometry= point.getGeometry();
        assertNotNull(pointGeometry);
        assertEquals(1, pointGeometry.numPoints());
        final Point pointGeometryPoint = pointGeometry.getPoint(0);
        assertEquals(34.0, pointGeometryPoint.getX(), 1e-8);
        assertEquals(11.6, pointGeometryPoint.getY(), 1e-8);
    }
}
