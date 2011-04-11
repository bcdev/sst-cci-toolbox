package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;

class ForwardingGeoCoding implements GeoCoding {

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
