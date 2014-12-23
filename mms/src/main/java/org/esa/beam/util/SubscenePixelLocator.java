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

import org.esa.beam.common.PixelLocator;

import java.awt.geom.Point2D;

/**
 * A simple {@link org.esa.beam.common.PixelLocator} implementation always finding the nearest pixel.
 *
 * @author Martin Boettcher
 * @author Ralf Quast
 * @author Thomas Storm
 */
final class SubscenePixelLocator extends AbstractPixelLocator {

    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private Point2D cachedPoint = null;
    private double cachedLon = Double.MAX_VALUE;
    private double cachedLat = Double.MAX_VALUE;

    static PixelLocator create(SampleSource lonSource, SampleSource latSource) {
        return new SubscenePixelLocator(lonSource, latSource);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param lonSource The source of longitude samples.
     * @param latSource The source of latitude samples.
     *
     * @throws IllegalArgumentException when the dimension of the sample sources are different.
     */
    private SubscenePixelLocator(SampleSource lonSource, SampleSource latSource) {
        super(lonSource, latSource);
    }


    @Override
    public boolean getPixelLocation(double lon, double lat, Point2D p) {
        if (lon == cachedLon && lat == cachedLat) {
            p.setLocation(cachedPoint);
            return true;
        }

        final int w = getLatSource().getWidth();
        final int h = getLatSource().getHeight();

        final double cosineFactor = Math.cos(lat * DEG_TO_RAD);
        int bestX = 0;
        int bestY = 0;
        double minDelta = Double.MAX_VALUE;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (getLatSource().isFillValue(x, y)) {
                    continue;
                }
                if (getLonSource().isFillValue(x, y)) {
                    continue;
                }
                final double sourceLat = getLat(x, y) + 90.0;
                final double sourceLon = getLon(x, y) + 180.0;
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
}
