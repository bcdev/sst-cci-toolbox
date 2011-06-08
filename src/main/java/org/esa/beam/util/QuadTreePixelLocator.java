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
import java.util.Arrays;
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
                final double lon = interpolate(x0, y0, wx, wy, lonSource);
                final double lat = interpolate(x0, y0, wx, wy, latSource);
                g.setLocation(lon, lat);
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
        if (w < 2 || h < 2) {
            return false;
        }
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int x0 = x;
        final int x1 = x0 + w - 1;
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        final int y0 = y;
        final int y1 = y0 + h - 1;

        if (w == 2 && h == 2) {
            final double lat0 = getLat(x0, y0);
            final double lat1 = getLat(x0, y1);
            final double lat2 = getLat(x1, y0);
            final double lat3 = getLat(x1, y1);

            final double lon0 = getLon(x0, y0);
            final double lon1 = getLon(x0, y1);
            final double lon2 = getLon(x1, y0);
            final double lon3 = getLon(x1, y1);

            final double f = Math.cos(lat * D2R);

            final double d0 = sqr(lat - lat0, f * Result.delta(lon, lon0));
            final double d1 = sqr(lat - lat1, f * Result.delta(lon, lon1));
            final double d2 = sqr(lat - lat2, f * Result.delta(lon, lon2));
            final double d3 = sqr(lat - lat3, f * Result.delta(lon, lon3));

            final boolean invalidated = result.invalidate(d0, d1, d2, d3);
            if (invalidated) {
                result.add(x0, y0, lon0, lat0, d0);
                result.add(x0, y1, lon1, lat1, d1);
                result.add(x1, y0, lon2, lat2, d2);
                result.add(x1, y1, lon3, lat3, d3);
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

        if (w2 < 2) {
            w2 = 2;
        }
        if (h2 < 2) {
            h2 = 2;
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
        return lonSource.getSample(x, y);
    }

    private double getLat(int x, int y) {
        return latSource.getSample(x, y);
    }

    private double interpolate(int x0, int y0, double wx, double wy, SampleSource sampleSource) {
        final int x1 = x0 + 1;
        final int y1 = y0 + 1;
        final double d00 = sampleSource.getSample(x0, y0);
        final double d10 = sampleSource.getSample(x1, y0);
        final double d01 = sampleSource.getSample(x0, y1);
        final double d11 = sampleSource.getSample(x1, y1);

        return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
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

        private final Rotation rotation;
        private final double[] deltas = new double[N];

        static double delta(double lon, double lon0) {
            final double e = Math.abs(lon - lon0);
            return e < 180.0 ? e : 360.0 - e;
        }

        Result(double lon, double lat) {
            rotation = new Rotation(lon, lat);
            Arrays.fill(deltas, Double.POSITIVE_INFINITY);
        }

        boolean invalidate(double d0, double d1, double d2, double d3) {
            if (d0 < getDelta() || d1 < getDelta() || d2 < getDelta() || d3 < getDelta()) {
                final boolean b0 = Double.isNaN(d0);
                final boolean b1 = Double.isNaN(d1);
                final boolean b2 = Double.isNaN(d2);
                final boolean b3 = Double.isNaN(d3);

                // allow a single NaN value
                if (!b1 && !b2 && !b3 || !b0 && !b2 && !b3 || !b0 && !b1 && !b3 || !b0 && !b1 && !b2) {
                    for (int i = 0; i < N; i++) {
                        xs[i] = 0.0;
                        ys[i] = 0.0;
                        lons[i] = 0.0;
                        lats[i] = 0.0;
                        deltas[i] = Double.MAX_VALUE;
                    }
                    return true;
                }
            }
            return false;
        }

        void add(int x, int y, double lon, double lat, double delta) {
            for (int i = 0; i < N; i++) {
                if (delta < deltas[i]) {
                    for (int k = N - 1; k > i; k--) {
                        xs[k] = xs[k - 1];
                        ys[k] = ys[k - 1];
                        lons[k] = lons[k - 1];
                        lats[k] = lats[k - 1];
                        deltas[k] = deltas[k - 1];
                    }
                    xs[i] = x + 0.5;
                    ys[i] = y + 0.5;
                    lons[i] = lon;
                    lats[i] = lat;
                    deltas[i] = delta;
                    break;
                }
            }
        }

        void get(Point2D p) {
            // todo - fix for situation when all lats or lons are equal
            if (lats[0] == lats[1] && lats[1] == lats[2]) {
                lats[1] += 0.001;
            }
            if (lons[0] == lons[1] && lons[1] == lons[2]) {
                lons[1] += 0.001;
            }
            rotation.transform(lons, lats);
            final RationalFunctionModel xModel = new RationalFunctionModel(1, 0, lons, lats, xs);
            final RationalFunctionModel yModel = new RationalFunctionModel(1, 0, lons, lats, ys);

            final double x = xModel.getValue(0.0, 0.0);
            final double y = yModel.getValue(0.0, 0.0);

            p.setLocation(x, y);
        }

        private double getDelta() {
            return deltas[0];
        }
    }
}
