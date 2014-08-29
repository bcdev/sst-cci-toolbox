package org.esa.beam.util;

import org.esa.beam.framework.datamodel.PixelLocator;
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
    public final void dispose() {
        latSource.dispose();
        lonSource.dispose();
    }

    @Override
    public final boolean getGeoLocation(double x, double y, Point2D g) {
        final int w = lonSource.getWidth();
        final int h = lonSource.getHeight();

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);

        if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
            if (x0 > 0 && x - x0 < 0.5 || x0 == w - 1) {
                x0 -= 1;
            }
            if (y0 > 0 && y - y0 < 0.5 || y0 == h - 1) {
                y0 -= 1;
            }
            final int x1 = x0 + 1;
            final int y1 = y0 + 1;
            if (x1 < w && y1 < h) {
                final double wx = x - (x0 + 0.5);
                final double wy = y - (y0 + 0.5);
                final Point2D p = interpolate(x0, y0, wx, wy);
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

    private Point2D interpolate(int x0, int y0, double wx, double wy) {
        final int x1 = x0 + 1;
        final int y1 = y0 + 1;

        final double[] lons = new double[4];
        lons[0] = getLon(x0, y0);
        lons[1] = getLon(x1, y0);
        lons[2] = getLon(x0, y1);
        lons[3] = getLon(x1, y1);

        final double[] lats = new double[4];
        lats[0] = getLat(x0, y0);
        lats[1] = getLat(x1, y0);
        lats[2] = getLat(x0, y1);
        lats[3] = getLat(x1, y1);

        final Point2D p = new Point2D.Double();
        if (Double.isNaN(lons[0]) || Double.isNaN(lons[1]) || Double.isNaN(lons[2]) || Double.isNaN(lons[3]) ||
            Double.isNaN(lats[0]) || Double.isNaN(lats[1]) || Double.isNaN(lats[2]) || Double.isNaN(lats[3])) {

            final int x = wx < 0.5 ? x0 : x1;
            final int y = wy < 0.5 ? y0 : y1;
            final double lon = getLon(x, y);
            final double lat = getLat(x, y);

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
