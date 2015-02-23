/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.util;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

/**
* @author Ralf Quast
*/
public class LocationTest {

    private int numCols;
    private int numRows;
    private GeoCoding geoCoding;
    private double lat;
    private double lon;
    private GeoPos geoPos;
    private PixelPos pixelPos;
    private int pixelX;
    private int pixelY;
    private boolean valid;

    public LocationTest(double lon, double lat, int numCols, int numRows, GeoCoding geoCoding) {
        this.lat = lat;
        this.lon = lon;
        this.numCols = numCols;
        this.numRows = numRows;
        this.geoCoding = geoCoding;
    }

    public GeoPos getGeoPos() {
        return geoPos;
    }

    public PixelPos getPixelPos() {
        return pixelPos;
    }

    public int getPixelX() {
        return pixelX;
    }

    public int getPixelY() {
        return pixelY;
    }

    public boolean isOK() {
        return valid;
    }

    public LocationTest invoke() {
        geoPos = new GeoPos((float) lat, (float) lon);
        pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
        pixelX = (int) Math.floor(pixelPos.getX());
        pixelY = (int) Math.floor(pixelPos.getY());
        valid = pixelPos.isValid() && pixelX > 0 && pixelY > 0 && pixelX < numCols - 1 && pixelY < numRows - 1;
        return this;
    }
}
