/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.cci.sst.reader;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple pixel geo-coding.
 * <p/>
 * This class basically is a stripped-down version of {@link org.esa.beam.framework.datamodel.PixelGeoCoding}
 * that avoids an issue (BEAM-1240) with full-orbit products, which affects TMI and AMSR-E.
 * <p/>
 * The simple pixel geo-coding is definitely slower than {@link org.esa.beam.framework.datamodel.PixelGeoCoding}
 * and must not be used for products that are displayed, reprojected, stored etc.
 *
 * @author Ralf Quast
 */
class SimplePixelGeoCoding extends AbstractGeoCoding {

    private static final double D2R = Math.PI / 180.0;

    private final Map<Rectangle, GeoRegion> regionMap =
            Collections.synchronizedMap(new HashMap<Rectangle, GeoRegion>());
    private final Band latBand;
    private final Band lonBand;

    /**
     * Constructs a new instance of this class.
     *
     * @param latBand The band, which provides the latitudes.
     * @param lonBand The band, which provides the longitudes.
     */
    SimplePixelGeoCoding(final Band latBand, final Band lonBand) {
        if (latBand.getProduct() == null) {
            throw new IllegalArgumentException("latBand.getProduct() == null");
        }
        if (lonBand.getProduct() == null) {
            throw new IllegalArgumentException("lonBand.getProduct() == null");
        }
        // If two bands are from the same product, they also have the same raster size
        if (latBand.getProduct() != lonBand.getProduct()) {
            throw new IllegalArgumentException("latBand.getProduct() != lonBand.getProduct()");
        }
        if (latBand.getProduct().getSceneRasterWidth() < 2) {
            throw new IllegalArgumentException("latBand.getProduct().getSceneRasterWidth() < 2");
        }
        if (latBand.getProduct().getSceneRasterHeight() < 2) {
            throw new IllegalArgumentException("latBand.getProduct().getSceneRasterHeight() < 2");
        }
        this.latBand = latBand;
        this.lonBand = lonBand;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public synchronized void dispose() {
    }

    @Override
    public Datum getDatum() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();

        if (pixelPos.isValid()) {
            final int x0 = (int) Math.floor(pixelPos.x);
            final int y0 = (int) Math.floor(pixelPos.y);
            final int w = latBand.getSceneRasterWidth();
            final int h = latBand.getSceneRasterHeight();

            if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
                final float lat = (float) getLat(x0, y0);
                final float lon = (float) getLon(x0, y0);
                geoPos.setLocation(lat, lon);
            }
        }

        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            getPixelPosUsingQuadTreeSearch(geoPos, pixelPos);
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        return false;
    }

    private void getPixelPosUsingQuadTreeSearch(final GeoPos geoPos, PixelPos pixelPos) {
        final Result result = new Result();
        final int w = latBand.getSceneRasterWidth();
        final int h = latBand.getSceneRasterHeight();
        final boolean pixelFound = quadTreeSearch(0, geoPos.lat, geoPos.lon, 0, 0, w, h, result);
        if (pixelFound) {
            pixelPos.setLocation(result.x + 0.5f, result.y + 0.5f);
        } else {
            pixelPos.setInvalid();
        }
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

    private double getLon(int x0, int y0) {
        return ProductUtils.getGeophysicalSampleDouble(lonBand, x0, y0, 0);
    }

    private double getLat(int x0, int y0) {
        return ProductUtils.getGeophysicalSampleDouble(latBand, x0, y0, 0);
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
}
