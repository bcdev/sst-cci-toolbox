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
 * An algorithm for finding the pixel position that corresponds to a given geo-location
 * and vice versa.
 *
 * @author Ralf Quast
 */
public interface PixelLocator {

    /**
     * Returns the (lon, lat) geo-location that corresponds to a given (x, y) pixel
     * location.
     *
     * @param x The pixel x location.
     * @param y The pixel y location.
     * @param g The geo-location. On return contains the geo-location position found. Is
     *          not modified, when the geo-location was not found.
     *
     * @return {@code true} if the geo-location was found, {@code false} otherwise.
     */
    public boolean getGeoLocation(double x, double y, Point2D g);

    /**
     * Returns the (x, y) pixel location that corresponds to a given (lon, lat) geo-location.
     *
     * @param lon The pixel longitude [-180.0, 180.0].
     * @param lat The pixel latitude [-90.0, 90.0].
     * @param p   The (x, y) pixel location. On return contains the pixel location found. Is
     *            not modified, when the pixel location was not found.
     *
     * @return {@code true} if the pixel location was found, {@code false} otherwise.
     */
    boolean getPixelLocation(double lon, double lat, Point2D p);
}
