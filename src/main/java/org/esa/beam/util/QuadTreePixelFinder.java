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
    private final double tolerance;

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
        // corresponds to 5 km at the equator, i.e. half a pixel for TMI and AMSR-E
        this.tolerance = 0.045;
    }

    @Override
    public boolean findPixel(double lon, double lat, Point2D pixelPos) {
        final Result result = new Result(lon, lat);
        final int w = latSource.getWidth();
        final int h = latSource.getHeight();
        final boolean pixelFound = quadTreeSearch(0, lat, lon, 0, 0, w, h, result);
        if (pixelFound) {
            result.get(pixelPos);
        }
        return pixelFound;
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

    private GeoRegion putNull(Rectangle pixelRegion) {
        regionMap.put(pixelRegion, null);
        return null;
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
}
