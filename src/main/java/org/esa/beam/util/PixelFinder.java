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

import java.awt.geom.Point2D;

/**
 * An algorithm for finding the pixel position that corresponds to a given geo-location.
 *
 * @author Ralf Quast
 */
public interface PixelFinder {

    /**
     * Finds the pixel position that corresponds to a given (lon, lat) geo-location.
     *
     * @param lon      The longitude [-180.0, 180.0].
     * @param lat      The latitude [-90.0, 90.0].
     * @param pixelPos The pixel position. On return contains the pixel position found. Is
     *                 not modified, when the pixel position was not found.
     *
     * @return {@code true} if the pixel position was found, {@code false} otherwise.
     */
    boolean findPixel(double lon, double lat, Point2D pixelPos);
}
