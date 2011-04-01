package org.esa.cci.sst.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PixelFinder} implementation using a quad-tree algorithm.
 *
 * @author Ralf Quast
 */
public class QuadTreePixelFinder implements PixelFinder {

    private static final double D2R = Math.PI / 180.0;

    private final Map<Rectangle, GeoRegion> regionMap =
            Collections.synchronizedMap(new HashMap<Rectangle, GeoRegion>());
    private final SampleSource lonSource;
    private final SampleSource latSource;

    /**
     * Constructs a new instance of this class.
     *
     * @param lonSource The source of longitude samples.
     * @param latSource The source of latitude samples.
     *
     * @throws IllegalArgumentException when the dimension of the sample sources are different.
     */
    public QuadTreePixelFinder(SampleSource lonSource, SampleSource latSource) {
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
    public boolean findPixel(double lon, double lat, Point2D pixelPos) {
        final Result result = new Result();
        final int w = latSource.getWidth();
        final int h = latSource.getHeight();
        final boolean pixelFound = quadTreeSearch(0, lat, lon, 0, 0, w, h, result);
        if (pixelFound) {
            pixelPos.setLocation(result.x + 0.5, result.y + 0.5);
        }
        return pixelFound;
    }

    private boolean quadTreeSearch(int depth, double lat, double lon, int x, int y, int w, int h, Result result) {
        if (w < 2 || h < 2) {
            return false;
        }
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int x1 = x;
        final int x2 = x1 + w - 1;
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int y1 = y;
        final int y2 = y1 + h - 1;

        final GeoRegion geoRegion = getGeoRegion(x1, x2, y1, y2);
        final double tolerance = 0.045; // corresponds to 5 km at the equator, i.e. half a pixel for TMI and AMSR-E
        if (!geoRegion.isOutside(lat, lon, tolerance)) {
            if (w == 2 && h == 2) {
                final double lat0 = getLat(x1, y1);
                final double lat1 = getLat(x1, y2);
                final double lat2 = getLat(x2, y1);
                final double lat3 = getLat(x2, y2);

                final double lon0 = getLon(x1, y1);
                final double lon1 = getLon(x1, y2);
                final double lon2 = getLon(x2, y1);
                final double lon3 = getLon(x2, y2);

                final double f = Math.cos(lat * D2R);
                if (result.update(x1, y1, sqr(lat - lat0, f * Result.delta(lon, lon0)))) {
                    return true;
                }
                if (result.update(x1, y2, sqr(lat - lat1, f * Result.delta(lon, lon1)))) {
                    return true;
                }
                if (result.update(x2, y1, sqr(lat - lat2, f * Result.delta(lon, lon2)))) {
                    return true;
                }
                if (result.update(x2, y2, sqr(lat - lat3, f * Result.delta(lon, lon3)))) {
                    return true;
                }
            } else if (w >= 2 && h >= 2) {
                return quadTreeRecursion(depth, lat, lon, x1, y1, w, h, result);
            }
        }

        return false;
    }

    private GeoRegion getGeoRegion(int x1, int x2, int y1, int y2) {
        final Rectangle pixelRegion = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);

        synchronized (regionMap) {
            if (!regionMap.containsKey(pixelRegion)) {
                double minLat = 90.0f;
                double maxLat = -90.0f;
                double minLon = 180.0f;
                double maxLon = -180.0f;
                boolean antimeridianIncluded = false;

                double lastLon1 = getLon(x1, y1);
                double lastLon2 = getLon(x2, y1);
                for (int y = y1; y <= y2; y++) {
                    final double lat1 = getLat(x1, y);
                    final double lat2 = getLat(x2, y);
                    minLat = min(lat1, minLat);
                    minLat = min(lat2, minLat);
                    maxLat = max(lat1, maxLat);
                    maxLat = max(lat2, maxLat);
                    final double lo1 = getLon(x1, y);
                    final double lo2 = getLon(x2, y);
                    minLon = min(lo1, minLon);
                    minLon = min(lo2, minLon);
                    maxLon = max(lo1, maxLon);
                    maxLon = max(lo2, maxLon);
                    if (!antimeridianIncluded) {
                        antimeridianIncluded = Math.abs(lastLon1 - lo1) > 180.0 || Math.abs(lastLon2 - lo2) > 180.0;
                        lastLon1 = lo1;
                        lastLon2 = lo2;
                    }
                }
                lastLon1 = getLon(x1, y1);
                lastLon2 = getLon(x1, y2);
                for (int x = x1; x <= x2; x++) {
                    final double lat1 = getLat(x, y1);
                    final double lat2 = getLat(x, y2);
                    minLat = min(lat1, minLat);
                    minLat = min(lat2, minLat);
                    maxLat = max(lat1, maxLat);
                    maxLat = max(lat2, maxLat);
                    final double lo1 = getLon(x, y1);
                    final double lo2 = getLon(x, y2);
                    minLon = min(lo1, minLon);
                    minLon = min(lo2, minLon);
                    maxLon = max(lo1, maxLon);
                    maxLon = max(lo2, maxLon);
                    if (!antimeridianIncluded) {
                        antimeridianIncluded = Math.abs(lastLon1 - lo1) > 180.0 || Math.abs(lastLon2 - lo2) > 180.0;
                        lastLon1 = lo1;
                        lastLon2 = lo2;
                    }
                }
                regionMap.put(pixelRegion, new GeoRegion(minLat, maxLat, minLon, maxLon, antimeridianIncluded));
            }

            return regionMap.get(pixelRegion);
        }
    }

    private boolean quadTreeRecursion(int depth, double lat, double lon, int i, int j, int w, int h, Result result) {
        int w2 = w >> 1;
        int h2 = h >> 1;

        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }
        if (h2 < 2) {
            h2 = 2;
        }

        final boolean b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }

    private double getLon(int x, int y) {
        return lonSource.getSample(x, y);
    }

    private double getLat(int x, int y) {
        return latSource.getSample(x, y);
    }

    private static class Result {

        public static final double INVALID = Double.MAX_VALUE;

        private int x;
        private int y;
        private double delta = INVALID;

        public final boolean update(int x, int y, double delta) {
            final boolean b = delta < this.delta;
            if (b) {
                this.x = x;
                this.y = y;
                this.delta = delta;
            }
            return b;
        }

        private static double delta(double lon, double lon0) {
            final double e = Math.abs(lon - lon0);
            if (e < 180.0) {
                return e;
            } else { // the Antimeridian is crossed
                return 360.0 - e;
            }
        }
    }

    private static class GeoRegion {

        final double minLat;
        final double maxLat;
        final double minLon;
        final double maxLon;
        final boolean antimeridianCrossed;

        private GeoRegion(double minLat, double maxLat, double minLon, double maxLon, boolean antimeridianCrossed) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.antimeridianCrossed = antimeridianCrossed;
        }

        /**
         * Conservatively tests if a point (lat, lon) is outside of the region, considering a
         * certain tolerance.
         *
         * @param lat       The latitude.
         * @param lon       The longitude.
         * @param tolerance The tolerance
         *
         * @return {@code true} if the point (lat, lon) is outside, {@code false} otherwise.
         */
        private boolean isOutside(double lat, double lon, double tolerance) {
            // be careful when expanding this expression into usage of if-else, it is critical for speed
            return lat < minLat - tolerance ||
                   lat > maxLat + tolerance ||
                   // do not evaluate the cosine expression unless it is needed
                   !antimeridianCrossed && (tolerance *= Math.cos(lat * D2R)) >= 0.0 && (lon < minLon - tolerance ||
                                                                                         lon > maxLon + tolerance);
        }
    }

    private static double min(double a, double b) {
        return (a <= b) ? a : b;
    }

    private static double max(double a, double b) {
        return (a >= b) ? a : b;
    }

    private static double sqr(double dx, double dy) {
        return dx * dx + dy * dy;
    }
}
