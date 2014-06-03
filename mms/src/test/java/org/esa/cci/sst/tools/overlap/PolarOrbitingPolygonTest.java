package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.GeometryUtil;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class PolarOrbitingPolygonTest {

    @Test
    public void testConstructor() {
        final Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        assertEquals("number of rings", 2, polygon.getRings().size());
        assertEquals("number of points in first ring", 12, polygon.getRings().get(0).size());
        assertEquals("number of points in second ring", 12, polygon.getRings().get(1).size());
        assertEquals("lat of first point of first ring", 58.0, polygon.getRings().get(0).get(0).getLat(), 1.0e-8);
    }

    @Test
    public void testIsPointInPolygon() {
        final Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        for (Point point : PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE) {
            assertTrue("lat=" + point.getY() + " lon=" + point.getX(), polygon.isPointInPolygon(point.getY(), point.getX()));
        }
        for (Point point : PolarOrbitingPolygonTestData.TEST_POINTS_OUTSIDE) {
            assertFalse("lat=" + point.getY() + " lon=" + point.getX(), polygon.isPointInPolygon(point.getY(), point.getX()));
        }
    }

    @Test
    public void testIsPointInRing() {
        Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        final List<PolarOrbitingPolygon.Point> points = polygon.getRings().get(0);
        assertFalse("lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getX(), polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getY(), PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getX(), points));
        assertFalse("lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getX(), polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getY(), PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getX(), points));
        assertTrue("lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getX(), polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getY(), PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getX(), points));
    }

    @Test
    public void testIsBetween() {
        assertTrue(PolarOrbitingPolygon.isBetween(13.0, 10.0, 20.0));
        assertTrue(PolarOrbitingPolygon.isBetween(13.0, 20.0, 10.0));

        assertFalse(PolarOrbitingPolygon.isBetween(13.0, 10.0, -14.0));
    }

    @Test
    public void testIsEdgeCrossingEquator() {
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingEquator(-1.0, 1.0));
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingEquator(1.0, -1.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(1.0, 1.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(-1.0, -1.0));

        assertTrue(PolarOrbitingPolygon.isEdgeCrossingEquator(-10.0, 10.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(-60.0, -30.0));
    }

    @Test
    public void testAtsr2Polygon() {
        final double skip = 2.5;               // 2.5
        final int expectedMatches = 519; // 3259
        final Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.ATSR2_POINTS)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);

        for (int rotations = 0; rotations < 360.0 / skip; ++rotations) {
            int orbitMatches = 0;
            for (double lat = -90.0 + skip; lat <= 90.0; lat += skip) {
                //int lineMatches = 0;
                for (double lon = -180.0 + skip; lon <= 180.0; lon += skip) {
                    if (polygon.isPointInPolygon(lat, lon)) {
                        ++orbitMatches;
                    }
                }
            }
            assertEquals("orbitMatches", expectedMatches, orbitMatches);
            rotate(PolarOrbitingPolygonTestData.ATSR2_POINTS, skip);
        }
    }

    private void rotate(Point[] points, double skip) {
        for (Point point : points) {
            point.setX(GeometryUtil.normalizeLongitude(point.getX() + skip));
        }
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(1.0, 1.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingEquator(-1.0, -1.0));
    }

    @Test
    public void testGetLongitudeAtEquator() {
        double longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 12.0, 14.0);
        assertEquals(13.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 0.0, 14.0, 15.0);
        assertEquals(-90.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 14.0, 15.0);
        assertEquals(1.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 14.0, 0.0);
        assertEquals(91.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 15.0, 14.0, 15.0);
        assertEquals(15.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(0.0, 1.0, 2.0, 3.0);
        assertEquals(1.0, longitudeAtEquator, 1e-8);

        assertEquals(10.0, PolarOrbitingPolygon.getLongitudeAtEquator(-20.0, 5.0, 40.0, 20.0), 1e-8);
        assertEquals(170.0, PolarOrbitingPolygon.getLongitudeAtEquator(20.0, 175.0, 60.0, -175), 1e-8);
    }

    @Test
    public void testGetLatitudeAtMeridian() {
        double latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(-19.0, 23.0, -21.0, 23.0);
        assertEquals(-19.0, latitudeAtMeridian, 1e-8);

        latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(-11.0, 23.0, -11.0, 26.0);
        assertEquals(-11.0, latitudeAtMeridian, 1e-8);

        latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(8.0, 0.0, -11.0, 26.0);
        assertEquals(8.0, latitudeAtMeridian, 1e-8);

        latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(-9.0, 10.0, -11.0, 12.0);
        assertEquals(1.0, latitudeAtMeridian, 1e-8);

        latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(-9.0, 10.0, -11.0, -12.0);
        assertEquals(-9.909090909090908, latitudeAtMeridian, 1e-8);

        latitudeAtMeridian = PolarOrbitingPolygon.getLatitudeAtMeridian(1.0, 2.0, 3.0, 4.0);
        assertEquals(-1.0, latitudeAtMeridian, 1e-8);

        assertEquals(40.0, PolarOrbitingPolygon.getLatitudeAtMeridian(50.0, -40.0, 35.0, 20.0), 1e-8);
        assertEquals(40.0, PolarOrbitingPolygon.getLatitudeAtMeridian(50.0, -40.0, 45.0, -20.0), 1e-8);
    }

    @Test
    public void testIsEdgeCrossingMeridian() {
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(-1.0, 1.0));
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(0.0, 1.0));
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(1.0, -1.0));
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(0.0, -1.0));

        assertFalse(PolarOrbitingPolygon.isEdgeCrossingMeridian(-1.0, -1.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingMeridian(1.0, 1.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingMeridian(-0.1, 182.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingMeridian(182.0, -0.1));

        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(-10.0, 10.0));
        assertTrue(PolarOrbitingPolygon.isEdgeCrossingMeridian(10.0, -10.0));
        assertFalse(PolarOrbitingPolygon.isEdgeCrossingMeridian(-160.0, 160.0));
    }

    @Test
    public void testGetId() throws SQLException {
        final Polygon polygon = new Polygon("POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(83, 14, polygon);

        assertEquals(83, polarOrbitingPolygon.getId());
    }

    @Test
    public void testGetTime() throws SQLException {
        final Polygon polygon = new Polygon("POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(30, 1983, polygon);

        assertEquals(1983, polarOrbitingPolygon.getTime());
    }

    @Test
    public void testRectangleInNorthernHemisphere() throws SQLException {
        // remember: WKT is (lon/lat) tb 2014-02-04
        final Polygon polygon = new Polygon("POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(12, 14, polygon);

        // center
        assertTrue(polarOrbitingPolygon.isPointInPolygon(31, 11));

        // lower left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(30.001, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(30.001, 9.998));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 9.998));

        // upper left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(30.001, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(30.001, 12.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 12.001));

        // upper right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(31.999, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(31.999, 12.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 12.001));

        // lower right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(31.999, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(31.999, 9.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 9.999));
    }

    @Test
    public void testRectangleInSouthernHemisphere() throws SQLException {
        // remember: WKT is (lon/lat) tb 2014-02-04
        final Polygon polygon = new Polygon("POLYGON((10 -50, 10.5 -50, 11 -50, 11.5 -50, 12 -50, 12 -50.5, 12 -51, 12 -51.5, 12 -52, 11.5 -52, 11 -52, 10.5 -52, 10 -52, 10 -51.5, 10 -51, 10 -50.5, 10 -50))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(12, 14, polygon);

        // center
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-51, 11));

        // lower left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-51.999, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-52.001, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-51.999, 9.998));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-52.001, 9.998));

        // upper left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-50.001, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-49.999, 10.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-50.001, 9.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-49.999, 9.999));

        // upper right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-50.001, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-49.999, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-50.001, 12.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-49.999, 12.001));

        // lower right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-51.999, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-52.001, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-51.999, 12.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-52.001, 12.001));
    }

    @Test
    public void testRhomboidOnEquator() throws SQLException {
        // remember: WKT is (lon/lat) tb 2014-02-05
        final Polygon polygon = new Polygon("POLYGON((-8 -2, -7.75 -1, -7.5 0, -7.25 1, -7 2, -6 2, -5 2, -4 2, -3 2, -3.25 1, -3.5 0, -3.75 -1, -4 -2, -5 -2, -6 -2, -7 -2, -8 -2))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(12, 14, polygon);

        // center
        assertTrue(polarOrbitingPolygon.isPointInPolygon(0, -5.5));

        // lower left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-1.95, -7.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-2.05, -7.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-1.95, -8.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-2.05, -8.05));

        // upper left
        assertTrue(polarOrbitingPolygon.isPointInPolygon(1.95, -6.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(2.05, -6.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(1.95, -7.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(2.05, -7.05));

        // upper right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(1.95, -3.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(2.05, -3.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(1.95, -2.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(2.05, -2.95));

        // lower right
        assertTrue(polarOrbitingPolygon.isPointInPolygon(-1.95, -4.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-2.05, -4.05));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-1.95, -3.95));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(-2.05, -3.95));
    }

    @Test
    public void testAvhrrPolygon() {
        final Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.AVHRR_POINTS)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);

        assertTrue(polygon.isPointInPolygon(6.79, -96.76));
        assertTrue(polygon.isPointInPolygon(-82.41, -112.76));
        assertTrue(polygon.isPointInPolygon(-80.34, 12.77));
        assertTrue(polygon.isPointInPolygon(-79.4, 127.0));
        assertTrue(polygon.isPointInPolygon(1.15, 120.98));
        assertTrue(polygon.isPointInPolygon(-24.64, 89.17));
        assertTrue(polygon.isPointInPolygon(-69.8, 19.35));
        assertTrue(polygon.isPointInPolygon(-25.39, -64.96));

        assertFalse(polygon.isPointInPolygon(5.66, -101.84));
        assertFalse(polygon.isPointInPolygon(-80.15, -113.14));
        assertFalse(polygon.isPointInPolygon(-60.21, 117.97));
        assertFalse(polygon.isPointInPolygon(-9.2, 123.8));
        assertFalse(polygon.isPointInPolygon(8.67, 101.22));
        assertFalse(polygon.isPointInPolygon(-25.2, -61.19));
    }
}
