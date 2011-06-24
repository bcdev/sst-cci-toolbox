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

package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.SampleSource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.Point2D;

/**
 * Geo-coding using a ${@link QuadTreePixelLocator}.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"deprecation"})
class PixelLocatorGeoCoding implements GeoCoding {

    private final QuadTreePixelLocator locator;

    PixelLocatorGeoCoding(SampleSource lonSource, SampleSource latSource) {
        this.locator = new QuadTreePixelLocator(lonSource, latSource);
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    @Override
    public boolean canGetPixelPos() {
        return false;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        final boolean success = locator.getPixelLocation(geoPos.getLon(), geoPos.getLat(), pixelPos);
        if (!success) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final Point2D.Float result = new Point2D.Float();
        final boolean success = locator.getGeoLocation(pixelPos.getX(), pixelPos.getY(), result);
        if (success) {
            geoPos.setLocation(result.y, result.x);
        } else {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public Datum getDatum() {
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return null;
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return null;
    }
}
