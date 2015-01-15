package org.esa.beam.util;

import org.esa.beam.common.PixelLocator;
import org.esa.beam.util.math.MathUtils;

import java.awt.geom.Point2D;

/**
 * @author Ralf Quast
 */
abstract class AbstractPixelLocator implements PixelLocator {

    private final SampleSource lonSource;
    private final SampleSource latSource;

    protected AbstractPixelLocator(SampleSource lonSource, SampleSource latSource) {
        if (lonSource.getWidth() != latSource.getWidth()) {
            throw new IllegalArgumentException("lonSource.getMaxX() != latSource.getMaxX()");
        }
        if (lonSource.getHeight() != latSource.getHeight()) {
            throw new IllegalArgumentException("lonSource.getMaxY() != latSource.getMaxY()");
        }
        this.lonSource = lonSource;
        this.latSource = latSource;
    }

    @Override
    public final boolean getGeoLocation(double x, double y, Point2D g) {
        final int w = lonSource.getWidth();
        final int h = lonSource.getHeight();

        final int x0 = (int) Math.floor(x);
        final int y0 = (int) Math.floor(y);

        if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
            int x1 = x0;
            int y1 = y0;
            if (x0 > 0 && x - x0 < 0.5 || x0 == w - 1) {
                x1 -= 1;
            }
            if (y0 > 0 && y - y0 < 0.5 || y0 == h - 1) {
                y1 -= 1;
            }
            final int x2 = x1 + 1;
            final int y2 = y1 + 1;
            if (x2 < w && y2 < h) {
                final double wx = x - (x1 + 0.5);
                final double wy = y - (y1 + 0.5);
                final Point2D p = interpolate(x0, y0, x1, y1, x2, y2, wx, wy);
                g.setLocation(p);
            } else {
                final double lon = getLon(x0, y0);
                final double lat = getLat(x0, y0);
                g.setLocation(lon, lat);
            }
            return true;
        }

        return false;
    }

    protected final double getLon(int x, int y) {
        final double lon = lonSource.getSample(x, y);
        if (lon > 180.0) {
            return lon - 360.0;
        }
        return lon;
    }

    protected final double getLat(int x, int y) {
        return latSource.getSample(x, y);
    }

    protected final SampleSource getLonSource() {
        return lonSource;
    }

    protected final SampleSource getLatSource() {
        return latSource;
    }

    private Point2D interpolate(int x0, int y0, int x1, int y1, int x2, int y2, double wx, double wy) {
        final double[] lons = new double[4];
        lons[0] = getLon(x1, y1);
        lons[1] = getLon(x2, y1);
        lons[2] = getLon(x1, y2);
        lons[3] = getLon(x2, y2);

        final double[] lats = new double[4];
        lats[0] = getLat(x1, y1);
        lats[1] = getLat(x2, y1);
        lats[2] = getLat(x1, y2);
        lats[3] = getLat(x2, y2);

        final Point2D p = new Point2D.Double();
        if (Double.isNaN(lons[0]) || Double.isNaN(lons[1]) || Double.isNaN(lons[2]) || Double.isNaN(lons[3]) ||
            Double.isNaN(lats[0]) || Double.isNaN(lats[1]) || Double.isNaN(lats[2]) || Double.isNaN(lats[3])) {

            final double lon = getLon(x0, y0);
            final double lat = getLat(x0, y0);

            p.setLocation(lon, lat);
        } else {
            final Rotation rotation = new Rotation(lons, lats);
            rotation.transform(lons, lats);

            final double lon = MathUtils.interpolate2D(wx, wy, lons[0], lons[1], lons[2], lons[3]);
            final double lat = MathUtils.interpolate2D(wx, wy, lats[0], lats[1], lats[2], lats[3]);

            p.setLocation(lon, lat);
            rotation.inverseTransform(p);
        }

        return p;
    }
}
