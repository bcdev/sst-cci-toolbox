package org.esa.beam.util;

import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class SubscenePixelLocatorTest {

    private SubscenePixelLocator locator;

    @Before
    public void setUp() {
        final double[][] lon = {{10.0, 11.0}, {10.0, 11.0}};
        final double[][] lat = {{20.0, 20.0}, {21.0, 21.0}};
        final TestSampleSource lonSource = new TestSampleSource(lon);
        final TestSampleSource latSource = new TestSampleSource(lat);
        locator = new SubscenePixelLocator(lonSource, latSource);
    }

    @Test
    public void testConstructorThrowsOnDifferentWidths() {
        final TestSampleSource lonSource = new TestSampleSource(new double[][]{{1.0}, {2.0}});
        final TestSampleSource latSource = new TestSampleSource(new double[][]{{3.0}, {4.0}, {5.0}});

        try {
            new SubscenePixelLocator(latSource, lonSource);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException expected) {
        }
    }

    @Test
    public void testConstructorThrowsOnDifferentHeights() {
        final TestSampleSource lonSource = new TestSampleSource(new double[][]{{1.0, 2.0}, {2.0, 3.0}});
        final TestSampleSource latSource = new TestSampleSource(new double[][]{{3.0}, {4.0}});

        try {
            new SubscenePixelLocator(latSource, lonSource);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetGeoLocation() {
        final Point2D.Double geoLocation = new Point2D.Double();

        assertTrue(locator.getGeoLocation(0, 0, geoLocation));
        assertEquals(9.500009617, geoLocation.getX(), 1e-8);
        assertEquals(19.497839052, geoLocation.getY(), 1e-8);

        assertTrue(locator.getGeoLocation(0.6, 0.8, geoLocation));
        assertEquals(10.299999477, geoLocation.getX(), 1e-8);
        assertEquals(20.10060273, geoLocation.getY(), 1e-8);

        assertTrue(locator.getGeoLocation(1, 1, geoLocation));
        assertEquals(10.5, geoLocation.getX(), 1e-8);
        assertEquals(20.500715669, geoLocation.getY(), 1e-8);

        assertTrue(locator.getGeoLocation(1.95, 1.98, geoLocation));
        assertEquals(11.4799915419, geoLocation.getX(), 1e-8);
        assertEquals(21.4479790238, geoLocation.getY(), 1e-8);
    }

    @Test
    public void testGetGeoLocation_x_y_outOfRange() {
        final Point2D.Double geoLocation = new Point2D.Double();

        assertFalse(locator.getGeoLocation(-0.01, 0.5, geoLocation));
        assertFalse(locator.getGeoLocation(0.5, -0.02, geoLocation));

        assertFalse(locator.getGeoLocation(2.01, 0.5, geoLocation));
        assertFalse(locator.getGeoLocation(0.5, 2.02, geoLocation));
    }

    @Test
    public void testGetGeoLocation_lonWrapsAt180Deg() {
        final double[][] lon = {{179, 181}, {179.5, 181.5}};
        final double[][] lat = {{20.0, 20.0}, {21.0, 21.0}};
        final TestSampleSource lonSource = new TestSampleSource(lon);
        final TestSampleSource latSource = new TestSampleSource(lat);
        final SubscenePixelLocator wrappingLocator = new SubscenePixelLocator(lonSource, latSource);

        final Point2D.Double geoLocation = new Point2D.Double();

        assertTrue(wrappingLocator.getGeoLocation(0.0, 0.0, geoLocation));
        assertEquals(177.752504965, geoLocation.getX(), 1e-8);
        assertEquals(19.49082406, geoLocation.getY(), 1e-8);

        assertTrue(wrappingLocator.getGeoLocation(1.0, 1.0, geoLocation));
        assertEquals(-179.75081572, geoLocation.getX(), 1e-8);
        assertEquals(20.503041839, geoLocation.getY(), 1e-8);
    }

    @Test
    public void testGetGeoLocation_handlesNaNs() {
        final double[][] lon = {{179, 181}, {179.5, 181.5}};
        final double[][] lat = {{20.0, 20.0}, {Double.NaN, 21.0}};
        final TestSampleSource lonSource = new TestSampleSource(lon);
        final TestSampleSource latSource = new TestSampleSource(lat);
        final SubscenePixelLocator nanLocator = new SubscenePixelLocator(lonSource, latSource);

        final Point2D.Double geoLocation = new Point2D.Double();

        assertTrue(nanLocator.getGeoLocation(0.0, 0.0, geoLocation));
        assertEquals(179.0, geoLocation.getX(), 1e-8);
        assertEquals(20.0, geoLocation.getY(), 1e-8);

        assertTrue(nanLocator.getGeoLocation(1.0, 1.0, geoLocation));
        assertEquals(-178.5, geoLocation.getX(), 1e-8);
        assertEquals(21.0, geoLocation.getY(), 1e-8);
    }

    @Test
    public void testGetPixelLocation() {
        final Point2D.Double pixelLocation = new Point2D.Double();

        assertTrue(locator.getPixelLocation(10, 20, pixelLocation));
        assertEquals(0.0, pixelLocation.getX(), 1e-8);
        assertEquals(0.0, pixelLocation.getY(), 1e-8);

        assertTrue(locator.getPixelLocation(11, 21, pixelLocation));
        assertEquals(1.0, pixelLocation.getX(), 1e-8);
        assertEquals(1.0, pixelLocation.getY(), 1e-8);
    }

    @Test
    public void testGetPixelLocation_cachedPoint() {
        final Point2D.Double pixelLocation = new Point2D.Double();

        assertTrue(locator.getPixelLocation(10, 20, pixelLocation));
        assertEquals(0.0, pixelLocation.getX(), 1e-8);
        assertEquals(0.0, pixelLocation.getY(), 1e-8);

        assertTrue(locator.getPixelLocation(10, 20, pixelLocation));
        assertEquals(0.0, pixelLocation.getX(), 1e-8);
        assertEquals(0.0, pixelLocation.getY(), 1e-8);
        // just an indirect check - at least that part of the cache-handling code is covered. Hard to check, though tb 2015-05-16
    }


    private class TestSampleSource implements SampleSource{

        private double[][] data;

        private TestSampleSource(double[][] data) {
            this.data = data;
        }

        @Override
        public int getWidth() {
            return data.length;
        }

        @Override
        public int getHeight() {
            return data[0].length;
        }

        @Override
        public double getSample(int x, int y) {
            return data[x][y];
        }

        @Override
        public boolean isFillValue(int x, int y) {
            return false;
        }
    }
}
