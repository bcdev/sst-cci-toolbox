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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;

public class ForwardingGeoCoding implements GeoCoding {

    private final GeoCoding delegate;

    public ForwardingGeoCoding(GeoCoding delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return delegate.isCrossingMeridianAt180();
    }

    @Override
    public boolean canGetPixelPos() {
        return delegate.canGetPixelPos();
    }

    @Override
    public boolean canGetGeoPos() {
        return delegate.canGetGeoPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        return delegate.getPixelPos(geoPos, pixelPos);
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        return delegate.getGeoPos(pixelPos, geoPos);
    }

    @Override
    public Datum getDatum() {
        return delegate.getDatum();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return delegate.getImageCRS();
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return delegate.getMapCRS();
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return delegate.getGeoCRS();
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return delegate.getImageToMapTransform();
    }
}
