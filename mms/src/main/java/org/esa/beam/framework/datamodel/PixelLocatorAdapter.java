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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.awt.geom.Point2D;

/**
 * An accurate geo-coding.
 *
 * @author Ralf Quast
 */
public final class PixelLocatorAdapter extends AbstractGeoCoding {

    private PixelLocator pixelLocator;

    public PixelLocatorAdapter(PixelLocator pixelLocator) {
        this.pixelLocator = pixelLocator;
    }

    @Override
    public GeoPos getGeoPos(PixelPos p, GeoPos g) {
        if (g == null) {
            g = new GeoPos();
        }
        if (!p.isValid() || !pixelLocator.getGeoLocation(p.getX(), p.getY(), new GeoPoint(g))) {
            g.setInvalid();
        }
        return g;
    }

    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }

    @Override
    public void dispose() {
        pixelLocator.dispose();
        pixelLocator = null;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
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
    public PixelPos getPixelPos(GeoPos g, PixelPos p) {
        if (p == null) {
            p = new PixelPos();
        }
        if (!g.isValid() || !pixelLocator.getPixelLocation(g.getLon(), g.getLat(), p)) {
            p.setInvalid();
        }
        return p;
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        return false;
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
