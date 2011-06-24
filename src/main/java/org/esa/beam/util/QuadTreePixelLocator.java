/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.util;

import org.esa.beam.framework.datamodel.RationalFunctionModel;
import org.esa.beam.util.math.MathUtils;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PixelLocator} implementation using a quad-tree algorithm.
 *
 * @author Martin Boettcher
 * @author Ralf Quast
 * @author Thomas Storm
 */
public class QuadTreePixelLocator implements PixelLocator {

    private static final double D2R = Math.PI / 180.0;

    private final Map<Rectangle, GeoRegion> regionMap =
            Collections.synchronizedMap(new HashMap<Rectangle, GeoRegion>());
    private final SampleSource lonSource;
    private final SampleSource latSource;
    private final double tolerance;
    private final int macroPixelSize;

    /**
     * Constructs a new instance of this class.
     *
     * @param lonSource The source of longitude samples.
     * @param latSource The source of latitude samples.
     *
     * @throws IllegalArgumentException when the dimension of the sample sources are different.
     */
    public QuadTreePixelLocator(SampleSource lonSource, SampleSource latSource) {
        if (lonSource.getWidth() != latSource.getWidth()) {
            throw new IllegalArgumentException("lonSource.getMaxX() != latSource.getMaxX()");
        }
        if (lonSource.getHeight() != latSource.getHeight()) {
            throw new IllegalArgumentException("lonSource.getMaxY() != latSource.getMaxY()");
        }
        this.lonSource = lonSource;
        this.latSource = latSource;
        // corresponds to 5 km at the equator, i.e. half a pixel for TMI and AMSR-E
        this.tolerance = 0.045;
        // suitable for Metop MD sub-scenes, can be less for other types
        this.macroPixelSize = 5;
    }


    @Override
    public boolean getGeoLocation(double x, double y, Point2D g) {
        final int w = lonSource.getWidth();
        final int h = lonSource.getHeight();

        final boolean cannotInterpolate = w == 1 && h == 1;
        if (cannotInterpolate) {
            final double lon = lonSource.getSample(0, 0);
            final double lat = latSource.getSample(0, 0);
            g.setLocation(lon, lat);
            return true;
        }

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

    @Override
    public boolean getPixelLocation(double lon, double lat, Point2D p) {
        final int w = latSource.getWidth();
        final int h = latSource.getHeight();
        final Result result = new Result(lon, lat);

        final boolean successful = quadTreeSearch(0, lat, lon, 0, 0, w, h, result);
        if (successful) {
            result.get(p);
        }
        return successful;
    }

    private boolean quadTreeSearch(int depth, double lat, double lon, int x, int y, int w, int h, Result result) {
        if (w < macroPixelSize || h < macroPixelSize) {
            return false;
        }
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int x0 = x;
        final int x1 = x0 + w - 1;
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int y0 = y;
        final int y1 = y0 + h - 1;

        if (w == macroPixelSize && h == macroPixelSize) {
            final double[] lats = new double[macroPixelSize * macroPixelSize];
            final double[] lons = new double[macroPixelSize * macroPixelSize];
            final double[] distances = new double[macroPixelSize * macroPixelSize];
            final double f = Math.cos(lat * D2R);

            for (int i = y0, ij = 0; i < y0 + macroPixelSize; i++) {
                for (int j = x0; j < x0 + macroPixelSize; j++, ij++) {
                    lats[ij] = getLat(j, i);
                    lons[ij] = getLon(j, i);
                    distances[ij] = sqr(lat - lats[ij], f * Result.deltaLon(lon, lons[ij]));
                }
            }

            final boolean invalidated = result.invalidate(distances);
            if (invalidated) {
                for (int i = y0, ij = 0; i < y0 + macroPixelSize; i++) {
                    for (int j = x0; j < x0 + macroPixelSize; j++, ij++) {
                        result.updateNearest(j, i, lons[ij], lats[ij], distances[ij]);
                    }
                }
                // todo - optimize: only a single point is needed at this stage of the algorithm
                for (int i = y0, ij = 0; i < y0 + macroPixelSize; i++) {
                    for (int j = x0; j < x0 + macroPixelSize; j++, ij++) {
                        result.updateFarthestLon(j, i, lons[ij], lats[ij]);
                    }
                }
                for (int i = y0, ij = 0; i < y0 + macroPixelSize; i++) {
                    for (int j = x0; j < x0 + macroPixelSize; j++, ij++) {
                        result.updateFarthest(j, i, lons[ij], lats[ij]);
                    }
                }
            }
            return invalidated;
        }
        if (w > 64 && h > 64) {
            final GeoRegion geoRegion = getGeoRegion(x0, x1, y0, y1);
            if (geoRegion != null && geoRegion.isOutside(lat, lon, tolerance)) {
                return false;
            }
        }
        return quadTreeRecursion(depth, lat, lon, x0, y0, w, h, result);
    }

    private GeoRegion getGeoRegion(int x0, int x1, int y0, int y1) {
        final Rectangle pixelRegion = new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1);

        synchronized (regionMap) {
            if (!regionMap.containsKey(pixelRegion)) {
                double minLat = 90.0;
                double maxLat = -90.0;
                double minLon = 180.0;
                double maxLon = -180.0;

                double lastLon0 = getLon(x0, y0);
                double lastLon1 = getLon(x1, y0);
                for (int y = y0; y <= y1; y++) {
                    final double lat0 = getLat(x0, y);
                    final double lat1 = getLat(x1, y);
                    final double lon0 = getLon(x0, y);
                    final double lon1 = getLon(x1, y);
                    if (Double.isNaN(lat0) || Double.isNaN(lat1) || Double.isNaN(lon0) || Double.isNaN(lon1)) {
                        return putNull(pixelRegion);
                    }
                    minLat = min(lat0, minLat);
                    minLat = min(lat1, minLat);
                    maxLat = max(lat0, maxLat);
                    maxLat = max(lat1, maxLat);
                    minLon = min(lon0, minLon);
                    minLon = min(lon1, minLon);
                    maxLon = max(lon0, maxLon);
                    maxLon = max(lon1, maxLon);
                    final boolean antimeridianIncluded = Math.abs(lastLon0 - lon0) > 180.0 || Math.abs(
                            lastLon1 - lon1) > 180.0;
                    if (antimeridianIncluded) {
                        return putNull(pixelRegion);
                    }
                    final boolean meridianIncluded = (lastLon0 > 0.0 != lon0 > 0.0) || (lastLon1 > 0.0 != lon1 > 0.0);
                    if (meridianIncluded) {
                        return putNull(pixelRegion);
                    }
                    lastLon0 = lon0;
                    lastLon1 = lon1;
                }
                lastLon0 = getLon(x0, y0);
                lastLon1 = getLon(x0, y1);
                for (int x = x0; x <= x1; x++) {
                    final double lat0 = getLat(x, y0);
                    final double lat1 = getLat(x, y1);
                    final double lon0 = getLon(x, y0);
                    final double lon1 = getLon(x, y1);
                    if (Double.isNaN(lat0) || Double.isNaN(lat1) || Double.isNaN(lon0) || Double.isNaN(lon1)) {
                        return putNull(pixelRegion);
                    }
                    minLat = min(lat0, minLat);
                    minLat = min(lat1, minLat);
                    maxLat = max(lat0, maxLat);
                    maxLat = max(lat1, maxLat);
                    minLon = min(lon0, minLon);
                    minLon = min(lon1, minLon);
                    maxLon = max(lon0, maxLon);
                    maxLon = max(lon1, maxLon);
                    final boolean antimeridianIncluded = Math.abs(lastLon0 - lon0) > 180.0 || Math.abs(
                            lastLon1 - lon1) > 180.0;
                    if (antimeridianIncluded) {
                        return putNull(pixelRegion);
                    }
                    final boolean meridianIncluded = (lastLon0 > 0.0 != lon0 > 0.0) || (lastLon1 > 0.0 != lon1 > 0.0);
                    if (meridianIncluded) {
                        return putNull(pixelRegion);
                    }
                    lastLon0 = lon0;
                    lastLon1 = lon1;
                }
                regionMap.put(pixelRegion, new GeoRegion(minLat, maxLat, minLon, maxLon));
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

        if (w2 < macroPixelSize) {
            w2 = macroPixelSize;
        }
        if (h2 < macroPixelSize) {
            h2 = macroPixelSize;
        }

        final boolean b1;
        final boolean b2;
        final boolean b3;
        final boolean b4;
        if (w >= 2 * h) {
            b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h, result);
            b2 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h, result);
            b3 = false;
            b4 = false;
        } else if (h >= 2 * w) {
            b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w, h2, result);
            b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w, h2r, result);
            b3 = false;
            b4 = false;
        } else {
            b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
            b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
            b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
            b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);
        }

        return b1 || b2 || b3 || b4;
    }

    private double getLon(int x, int y) {
        final double lon = lonSource.getSample(x, y);
        if (lon > 180.0) {
            return lon - 360.0;
        }
        return lon;
    }

    private double getLat(int x, int y) {
        return latSource.getSample(x, y);
    }

    private Point2D interpolate(int x0, int y0, double wx, double wy) {
        final int x1 = x0 + 1;
        final int y1 = y0 + 1;
        final double[] lons = new double[4];
        final double[] lats = new double[4];

        lons[0] = getLon(x0, y0);
        lons[1] = getLon(x1, y0);
        lons[2] = getLon(x0, y1);
        lons[3] = getLon(x1, y1);

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

    private GeoRegion putNull(Rectangle pixelRegion) {
        regionMap.put(pixelRegion, null);
        return null;
    }

    private static class GeoRegion {

        private final double minLat;
        private final double maxLat;
        private final double minLon;
        private final double maxLon;

        private GeoRegion(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }

        /**
         * Conservatively tests if a point (lat, lon) is outside of the region, considering a
         * certain tolerance.
         * <p/>
         * Note that this test does not yield the expected results when the region contains a
         * pole.
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
                   (tolerance *= Math.cos(
                           lat * D2R)) >= 0.0 && (lon < minLon - tolerance || lon > maxLon + tolerance);
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

    private static final class Result {

        private static final int N = 3;

        private final double[] xs = new double[N];
        private final double[] ys = new double[N];
        private final double[] lons = new double[N];
        private final double[] lats = new double[N];

        private final double lon;
        private final double lat;
        private final Rotation rotation;
        private double distance = Double.POSITIVE_INFINITY;

        static double deltaLon(double lon, double lon0) {
            final double d = Math.abs(lon - lon0);
            return d < 180.0 ? d : 360.0 - d;
        }

        Result(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
            this.rotation = new Rotation(lon, lat);
        }

        boolean invalidate(double... distances) {
            for (int i = 0, nanCount = 0, dsLength = distances.length; i < dsLength; i++) {
                if (Double.isNaN(distances[i])) {
                    nanCount++;
                }
                if (nanCount > distances.length - N) {
                    return false;
                }
            }
            for (int i = 0, dsLength = distances.length; i < dsLength; i++) {
                if (distances[i] < distance) {
                    for (int k = 0; k < N; k++) {
                        xs[k] = 0.0;
                        ys[k] = 0.0;
                        lons[k] = lon;
                        lats[k] = lat;
                        distance = Double.POSITIVE_INFINITY;
                    }
                    return true;
                }
            }
            return false;
        }

        private boolean isFartherLon(double lon) {
            return deltaLon(lons[0], lon) > deltaLon(lons[0], lons[1]);
        }

        private boolean isFarther(double lon, double lat) {
            final Point2D.Double a = new Point2D.Double(lons[0], lats[0]);
            final Point2D.Double b = new Point2D.Double(lons[1], lats[1]);
            final Point2D.Double c = new Point2D.Double(lons[2], lats[2]);
            final Point2D.Double p = new Point2D.Double(lon, lat);

            rotation.transform(a);
            rotation.transform(b);
            rotation.transform(c);
            rotation.transform(p);

            final double apx = a.getX() - p.getX();
            final double apy = a.getY() - p.getY();
            final double bpx = b.getX() - p.getX();
            final double bpy = b.getY() - p.getY();
            final double abx = a.getX() - b.getX();
            final double aby = a.getY() - b.getY();
            final double acx = a.getX() - c.getX();
            final double acy = a.getY() - c.getY();
            final double bcx = b.getX() - c.getX();
            final double bcy = b.getY() - c.getY();

            final double ap = apx * apx + apy * apy;
            final double bp = bpx * bpx + bpy * bpy;
            final double ab = abx * abx + aby * aby;
            final double ac = acx * acx + acy * acy;
            final double bc = bcx * bcx + bcy * bcy;

            return (ap + bp) / (ab + Math.abs(ap - bp)) > (ac + bc) / (ab + Math.abs(ac - bc));
        }

        void get(Point2D p) {
            rotation.transform(lons, lats);
            final RationalFunctionModel xModel = new RationalFunctionModel(1, 0, lons, lats, xs);
            final RationalFunctionModel yModel = new RationalFunctionModel(1, 0, lons, lats, ys);

            final double x = xModel.getValue(0.0, 0.0);
            final double y = yModel.getValue(0.0, 0.0);

            p.setLocation(x, y);
        }

        public void updateNearest(int x, int y, double lon, double lat, double d) {
            if (d < distance) {
                xs[0] = xs[1] = xs[2] = x + 0.5;
                ys[0] = ys[1] = ys[2] = y + 0.5;
                lons[0] = lons[2] = lons[2] = lon;
                lats[0] = lats[1] = lats[2] = lat;
                distance = d;
            }
        }

        public void updateFarthestLon(int x, int y, double lon, double lat) {
            if (isFartherLon(lon)) {
                xs[1] = x + 0.5;
                ys[1] = y + 0.5;
                lons[1] = lon;
                lats[1] = lat;
            }
        }

        public void updateFarthest(int x, int y, double lon, double lat) {
            if (isFarther(lon, lat)) {
                xs[2] = x + 0.5;
                ys[2] = y + 0.5;
                lons[2] = lon;
                lats[2] = lat;
            }
        }
    }
}
