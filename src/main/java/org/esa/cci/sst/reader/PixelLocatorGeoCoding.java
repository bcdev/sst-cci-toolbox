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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.SampleSource;

import java.awt.geom.Point2D;

/**
* Geo-coding using a ${@link QuadTreePixelLocator}.
*
* @author Thomas Storm
*/
@SuppressWarnings({"deprecation"})
class PixelLocatorGeoCoding extends AbstractGeoCoding {

    private final QuadTreePixelLocator locator;

    PixelLocatorGeoCoding(SampleSource lonSource, SampleSource latSource) {
        this.locator = new QuadTreePixelLocator(lonSource, latSource);
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        return false;
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
        final Point2D.Double point = new Point2D.Double();
        locator.getPixelLocation(geoPos.lon, geoPos.lat, point);
        final PixelPos result = new PixelPos();
        result.setLocation(point);
        return result;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final Point2D.Double result = new Point2D.Double();
        locator.getGeoLocation(pixelPos.x, pixelPos.y, result);
        geoPos.setLocation((float) result.y, (float) result.x);
        return geoPos;
    }

    @Override
    public Datum getDatum() {
        return null;
    }

    @Override
    public void dispose() {
    }
}
