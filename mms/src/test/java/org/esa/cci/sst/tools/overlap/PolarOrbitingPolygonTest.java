package org.esa.cci.sst.tools.overlap;


import org.junit.Test;
import org.postgis.Polygon;

import java.sql.SQLException;

import static org.junit.Assert.*;

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

    @Test
    public void testGetLongitudeAtEquator() {
        double longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 12.0, 14.0);
        assertEquals(13.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 0.0, 14.0, 15.0);
        assertEquals(-90.0, longitudeAtEquator, 1e-8);  // @todo 1 tb/mb really?

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 14.0, 15.0);
        assertEquals(1.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 13.0, 14.0, 0.0);
        assertEquals(91.0, longitudeAtEquator, 1e-8);   // @todo 1 tb/mb really?

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(12.0, 15.0, 14.0, 15.0);
        assertEquals(15.0, longitudeAtEquator, 1e-8);

        longitudeAtEquator = PolarOrbitingPolygon.getLongitudeAtEquator(0.0, 1.0, 2.0, 3.0);
        assertEquals(1.0, longitudeAtEquator, 1e-8);
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


        assertTrue(polarOrbitingPolygon.isPointInPolygon(30.001, 10.002));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 10.002));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(30.001, 9.998));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(29.999, 9.998));


        assertTrue(polarOrbitingPolygon.isPointInPolygon(31.999, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 11.999));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(31.999, 12.001));
        assertFalse(polarOrbitingPolygon.isPointInPolygon(32.001, 12.001));
    }
}
