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

import org.esa.beam.util.math.MathUtils;

import javax.persistence.NamedNativeQueries;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * A {@link org.esa.beam.util.PixelLocator} implementation using a quad-tree algorithm.
 *
 * @author Martin Boettcher
 * @author Ralf Quast
 * @author Thomas Storm
 */
public class SimplePixelLocator implements PixelLocator {

    private static final double DEG_TO_RAD = Math.PI / 180.0;

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
    public SimplePixelLocator(SampleSource lonSource, SampleSource latSource) {
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
    public boolean getGeoLocation(double x, double y, Point2D g) {
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

    private Point2D cachedPoint = null;
    private double cachedLon = Double.MAX_VALUE;
    private double cachedLat = Double.MAX_VALUE;


    @Override
    public boolean getPixelLocation(double lon, double lat, Point2D p) {
        if (lon == cachedLon && lat == cachedLat) {
            p.setLocation(cachedPoint);
            return true;
        }

        final int w = latSource.getWidth();
        final int h = latSource.getHeight();

        final double cosineFactor = Math.cos(lat * DEG_TO_RAD);
        int bestX = 0;
        int bestY = 0;
        double minDelta = Double.MAX_VALUE;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final double sourceLat = getLat(x, y) + 90.0;
                if(latSource.getFillValue() != null && sourceLat == latSource.getFillValue().doubleValue()) {
                    continue;
                }
                final double sourceLon = getLon(x, y) + 180.0;
                if(lonSource.getFillValue() != null && sourceLon == lonSource.getFillValue().doubleValue()) {
                    continue;
                }
                final double delta = (sourceLat - (lat + 90.0)) * (sourceLat - (lat + 90.0)) +
                                     (sourceLon - (lon + 180.0)) * (sourceLon - (lon + 180.0)) * cosineFactor;
                if (delta < minDelta) {
                    bestX = x;
                    bestY = y;
                    minDelta = delta;
                }
            }
        }
        p.setLocation(bestX, bestY);
        cachedLat = lat;
        cachedLon = lon;
        cachedPoint = p;
        return true;
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
