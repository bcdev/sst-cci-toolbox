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
