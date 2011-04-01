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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.cci.sst.util.PixelFinder;
import org.esa.cci.sst.util.SampleSource;
import org.esa.cci.sst.util.QuadTreePixelFinder;


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

    private final SampleSource latSource;
    private final SampleSource lonSource;
    private final PixelFinder pixelFinder;

    /**
     * Constructs a new instance of this class.
     *
     * @param latSource The band, which provides the latitudes.
     * @param lonSource The band, which provides the longitudes.
     */
    SimplePixelGeoCoding(final SampleSource latSource, final SampleSource lonSource) {
        this.pixelFinder = new QuadTreePixelFinder(lonSource, latSource);
        if (latSource.getMaxX() < 2) {
            throw new IllegalArgumentException("latBand.getWidth() < 2");
        }
        if (latSource.getMaxY() < 2) {
            throw new IllegalArgumentException("latBand.getHeight() < 2");
        }
        this.latSource = latSource;
        this.lonSource = lonSource;
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
            final int w = latSource.getMaxX();
            final int h = latSource.getMaxY();

            if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
                final float lat = (float) latSource.getSample(x0, y0);
                final float lon = (float) lonSource.getSample(x0, y0);
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
            final boolean pixelFound = pixelFinder.findPixel(geoPos.getLon(), geoPos.getLat(), pixelPos);
            if (!pixelFound) {
                pixelPos.setInvalid();
            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        return false;
    }

}
