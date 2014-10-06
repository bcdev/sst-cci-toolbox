package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.tools.samplepoint.SobolSamplePointGenerator;
import org.esa.cci.sst.util.GeometryUtil;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PolarOrbitingPolygonTest {

    @Test
    public void testConstructor() {
        final Geometry geometry = new Polygon(
                new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        assertEquals("number of rings", 2, polygon.getRings().size());
        assertEquals("number of points in first ring", 16, polygon.getRings().get(0).size());
        assertEquals("number of points in second ring", 8, polygon.getRings().get(1).size());
        assertEquals("lat of first point of first ring", 38.4, polygon.getRings().get(0).get(0).getLat(), 1.0e-8);
    }

    @Test
    public void testIsPointInPolygon() {
        final Geometry geometry = new Polygon(
                new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        for (Point point : PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE) {
            assertTrue("lat=" + point.getY() + " lon=" + point.getX(),
                       polygon.isPointInPolygon(point.getY(), point.getX()));
        }
        for (Point point : PolarOrbitingPolygonTestData.TEST_POINTS_OUTSIDE) {
            assertFalse("lat=" + point.getY() + " lon=" + point.getX(),
                        polygon.isPointInPolygon(point.getY(), point.getX()));
        }
    }

    @Test
    public void testIsPointInRing() {
        Geometry geometry = new Polygon(new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.TEST_POLYGON)});
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, System.currentTimeMillis(), geometry);
        final List<PolarOrbitingPolygon.Point> points = polygon.getRings().get(0);
        assertFalse(
                "lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getX(),
                polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getY(),
                                      PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[0].getX(), points));
        assertTrue(
                "lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getX(),
                polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getY(),
                                      PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[1].getX(), points));
        assertTrue(
                "lat=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getY() + " lon=" + PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getX(),
                polygon.isPointInRing(PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getY(),
                                      PolarOrbitingPolygonTestData.TEST_POINTS_INSIDE[2].getX(), points));
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
        final Geometry geometry = new Polygon(
                new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.ATSR2_POINTS)});
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
        final Polygon polygon = new Polygon(
                "POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(83, 14, polygon);

        assertEquals(83, polarOrbitingPolygon.getId());
    }

    @Test
    public void testGetTime() throws SQLException {
        final Polygon polygon = new Polygon(
                "POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
        final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(30, 1983, polygon);

        assertEquals(1983, polarOrbitingPolygon.getTime());
    }

    @Test
    public void testRectangleInNorthernHemisphere() throws SQLException {
        // remember: WKT is (lon/lat) tb 2014-02-04
        final Polygon polygon = new Polygon(
                "POLYGON((10 30, 10.5 30, 11 30, 11.5 30, 12 30, 12 30.5, 12 31, 12 31.5, 12 32, 11.5 32, 11 32, 10.5 32, 10 32, 10 31.5, 10 31, 10 30.5, 10 30))");
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
        final Polygon polygon = new Polygon(
                "POLYGON((10 -50, 10.5 -50, 11 -50, 11.5 -50, 12 -50, 12 -50.5, 12 -51, 12 -51.5, 12 -52, 11.5 -52, 11 -52, 10.5 -52, 10 -52, 10 -51.5, 10 -51, 10 -50.5, 10 -50))");
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
        final Polygon polygon = new Polygon(
                "POLYGON((-8 -2, -7.75 -1, -7.5 0, -7.25 1, -7 2, -6 2, -5 2, -4 2, -3 2, -3.25 1, -3.5 0, -3.75 -1, -4 -2, -5 -2, -6 -2, -7 -2, -8 -2))");
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
        final Geometry geometry = new Polygon(
                new LinearRing[]{new LinearRing(PolarOrbitingPolygonTestData.AVHRR_POINTS)});
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

    @Test
    public void testAvhrrPolygon2() throws SQLException {
        final Geometry geometry = new Polygon(
                "POLYGON((-9.28800010681152 16,-11.2559995651245 5.64400005340576,-13.6809997558594 -4.625,-16.5690002441406 -14.793999671936,-20.1690006256104 -24.806001663208,-24.75 -34.5880012512207,-30.9309997558594 -43.9879989624023,-39.6559982299805 -52.8250007629395,-52.7880020141602 -6" +
                "0.5060043334961,-72.7750015258789 -66.0439987182617,-99.4619979858398 -67.7440032958984,-124.813003540039 -64.8130035400391,-142.781005859375 -58.5750045776367,-154.468994140625 -50.4440002441406,-162.369003295898 -41.4440002441406,-168.024993896484 -31.8689994812012,-172.25601" +
                "1962891 -21.9370002746582,-175.619003295898 -11.8190002441406,-178.388000488281 -1.57500004768372,179.305999755859 8.77499961853027,177.44401550293 19.1500015258789,175.988006591797 29.5809993743896,174.880996704102 40.0250015258789,174.350006103516 50.3940010070801,174.8250122" +
                "07031 60.7810020446777,177.856002807617 71.0059967041016,178.106002807617 71.5690002441406,149.986999511719 71.2940063476562,138.963012695312 69.9370040893555,129.805999755859 68.0440063476562,115.812004089355 63.0750045776367,113.869003295898 62.1000022888184,127.831001281738 " +
                "54.3380012512207,136.830993652344 45.3499984741211,143.080993652344 35.8129997253418,147.669006347656 25.8940010070801,151.225006103516 15.7810001373291,154.074996948242 5.51300001144409,156.406005859375 -4.84999990463257,158.287002563477 -15.206000328064,159.888000488281 -25.6" +
                "380004882812,161.069000244141 -36.0250015258789,161.688003540039 -46.3689994812012,161.537002563477 -56.6560020446777,159.731002807617 -66.8690032958984,152.444000244141 -76.8059997558594,108.237998962402 -85.0189971923828,24.6370010375977 -80.5620040893555,11.1879997253418 -70" +
                ".8499984741211,8.05000019073486 -60.7750015258789,7.39400005340576 -50.5060005187988,7.79400014877319 -40.1750030517578,8.78800010681152 -29.8560009002686,10.1560001373291 -19.5060005187988,11.9130001068115 -9.125,14.0310010910034 1.18700003623962,16.6499996185303 11.4750003814" +
                "697,16.8190002441406 12.0190000534058,8.125 13.6810007095337,4.16900014877319 14.331000328064,0.263000011444092 14.9000005722046,-7.83100032806396 15.8620004653931,-9.28800010681152 16))");
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, 0, geometry);

        assertTrue(polygon.isPointInPolygon(11.5, 2.5));
        assertTrue(polygon.isPointInPolygon(-20.5, -3.5));
        assertTrue(polygon.isPointInPolygon(-58.5, -18.5));
        assertTrue(polygon.isPointInPolygon(-78.5, -52.4));
        assertTrue(polygon.isPointInPolygon(-567.5, -174.5));
        assertTrue(polygon.isPointInPolygon(61.5, 147.5));
        assertTrue(polygon.isPointInPolygon(2.5, 167.5));

        assertFalse(polygon.isPointInPolygon(-70.5, 17.5));
        assertFalse(polygon.isPointInPolygon(-50.6, 49.5));
        assertFalse(polygon.isPointInPolygon(-7.7, 115.5));
        assertFalse(polygon.isPointInPolygon(-33.5, -43.5));
        assertFalse(polygon.isPointInPolygon(-32.5, -158.5));
        assertFalse(polygon.isPointInPolygon(-22.5, -94.5));
        assertFalse(polygon.isPointInPolygon(75.5, -61.5));
    }

    public static void main(String[] args) throws IOException, SQLException {
        final Geometry geometry = new Polygon(
                "POLYGON((-105.894004821777 82.5810012817383,-82.0559997558594 69.0749969482422,-79.2060012817383 54.6060028076172,-79.5749969482422 40,-81.281005859375 25.3440017700195,-83.75 10.7189998626709,-87.038002" +
                "0141602 -3.76900029182434,-91.375 -18.0690002441406,-97.3560028076172 -31.9620018005371,-106.21900177002 -45.1310005187988,-120.894004821777 -56.8560028076172,-146.600006103516 -65.0879974365234,177.094009399414 -66.0690" +
                "002441406,148.343994140625 -59.0810012817383,131.781005859375 -47.9310035705566,121.96900177002 -34.9749984741211,115.506004333496 -21.2190017700195,110.862998962402 -7,107.362007141113 7.4370002746582,104.744003295898 2" +
                "2.0249996185303,102.794006347656 36.669002532959,102.069000244141 51.2999992370605,103.763008117676 65.8120040893555,117.444000244141 79.7559967041016,-132.256011962891 82.7120056152344,-107.65599822998 69.2559967041016," +
                "-107.563003540039 69.1190032958984,-81.2690048217773 68.8190002441406,-70.9309997558594 67.5130004882812,-62.1560020446777 65.6809997558594,-48.1500015258789 60.7440032958984,-46.1190032958984 59.7120018005371,-16.493999" +
                "4812012 67.1060028076172,21.7130012512207 66.0440063476562,47.9440002441406 57.4129981994629,62.3060035705566 45.294002532959,70.8690032958984 31.875,76.5060043334961 17.7810001373291,80.6500015258789 3.39400005340576,83" +
                ".7750015258789 -11.1620006561279,86.0810012817383 -25.75,87.5250015258789 -40.3000030517578,87.6689987182617 -54.8380012512207,84.4310073852539 -69.0940017700195,59.8499984741211 -82.375,-47.5250015258789 -79.95000457763" +
                "67,-62.7690010070801 -66.2129974365234,-64.9750061035156 -51.8690032958984,-64.4619979858398 -37.375,-62.7880020141602 -22.7310009002686,-60.3310012817383 -8.18099975585938,-57.1000022888184 6.38100051879883,-52.66899871" +
                "82617 20.7059993743896,-46.5500030517578 34.7190017700195,-37.1059989929199 47.9500007629395,-20.8750019073486 59.6060028076172,8.58100032806396 67.0630035400391,8.91899967193604 67.0940017700195,-0.531000018119812 75.65" +
                "00015258789,-9.34400081634521 79.2190017700195,-25.3880004882812 82.2500076293945,-95.3810043334961 83.4310073852539,-105.894004821777 82.5810012817383))");
        final PolarOrbitingPolygon polygon = new PolarOrbitingPolygon(1, 0, geometry);

        final SobolSamplePointGenerator g = new SobolSamplePointGenerator();
        final List<SamplingPoint> allPoints = g.createSamples(50000, 0, 0, 1);
        final List<SamplingPoint> interiorPoints = new ArrayList<>(allPoints.size());
        for (SamplingPoint p : allPoints) {
            if (polygon.isPointInPolygon(p.getLat(), p.getLon())) {
                interiorPoints.add(p);
            }
        }

        final SamplingPointPlotter plotter = new SamplingPointPlotter();
        plotter.samples(interiorPoints)
                .live(true)
                .show(true)
                .plot();
    }

}
