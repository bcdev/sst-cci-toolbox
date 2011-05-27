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

package org.esa.beam.framework.datamodel;

import org.esa.beam.util.PixelLocator;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.RasterDataNodeSampleSource;

import java.awt.geom.Point2D;

/**
 * A geo-coding working around BEAM-1240 and providing sub-pixel precision.
 *
 * @author Ralf Quast
 */
public class PixelGeoCodingWrapper extends ForwardingGeoCoding {

    private final PixelLocator pixelLocator;

    public PixelGeoCodingWrapper(PixelGeoCoding pixelGeoCoding) {
        super(pixelGeoCoding);
        final RasterDataNodeSampleSource lonSource = new RasterDataNodeSampleSource(pixelGeoCoding.getLonBand());
        final RasterDataNodeSampleSource latSource = new RasterDataNodeSampleSource(pixelGeoCoding.getLatBand());
        pixelLocator = new QuadTreePixelLocator(lonSource, latSource);
    }

    @Override
    public final GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        if (pixelPos.isValid()) {
            pixelLocator.getGeoLocation(pixelPos.getX(), pixelPos.getY(), new GeoPoint(geoPos));
        } else {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public final PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            pixelLocator.getPixelLocation(geoPos.getLon(), geoPos.getLat(), pixelPos);
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    private static class GeoPoint extends Point2D {

        private final GeoPos g;

        public GeoPoint(GeoPos g) {
            this.g = g;
        }

        @Override
        public final double getX() {
            return g.lon;
        }

        @Override
        public final double getY() {
            return g.lat;
        }

        @Override
        public final void setLocation(double x, double y) {
            g.lon = (float) x;
            g.lat = (float) y;
        }
    }
}
