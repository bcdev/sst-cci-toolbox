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

import org.esa.beam.util.PixelFinder;

/**
 * Workaround for BEAM-1241
 *
 * @author Ralf Quast
 */
public class TiePointGeoCodingWithFallback extends ForwardingGeoCoding {

    private final int sceneRasterWidth;
    private final int sceneRasterHeight;
    private final PixelFinder pixelFinder;

    public TiePointGeoCodingWithFallback(TiePointGeoCoding tiePointGeoCoding, PixelFinder pixelFinder) {
        super(tiePointGeoCoding);
        sceneRasterWidth = tiePointGeoCoding.getLatGrid().getSceneRasterWidth();
        sceneRasterHeight = tiePointGeoCoding.getLatGrid().getSceneRasterHeight();
        this.pixelFinder = pixelFinder;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        super.getPixelPos(geoPos, pixelPos);
        if (geoPos.isValid()) {
            if (!pixelPos.isValid() ||
                pixelPos.x < 0 || pixelPos.y < 0 ||
                pixelPos.x > sceneRasterWidth || pixelPos.y > sceneRasterHeight) {
                final boolean pixelFound = pixelFinder.findPixel(geoPos.getLon(), geoPos.getLat(), pixelPos);
                if (!pixelFound) {
                    pixelPos.setInvalid();
                }
            }
        }
        return pixelPos;
    }
}
