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

package org.esa.cci.sst.util;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates pixel and geo boundaries for a given product. The cases where a
 * product contains leading and trailing rows (or columns) of pixels where
 * the geo-location is invalid are handled.
 *
 * @author Ralf Quast
 */
public class BoundaryCalculator {

    /**
     * Returns the geo-boundary of a product. The geo-boundary shall enclose only
     * pixels where the geo-location is valid.
     *
     * @param product The product.
     *
     * @return the geo-boundary of the product supplied as argument.
     *
     * @throws Exception when the geo-boundary cannot not be calculated.
     */
    public PGgeometry getGeoBoundary(Product product) throws Exception {
        final Rectangle boundary = getPixelBoundary(product);
        final int minX = boundary.x;
        final int minY = boundary.y;
        final int maxX = boundary.x + boundary.width - 1;
        final int maxY = boundary.y + boundary.height - 1;
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        final int stepX = Math.max(100, w / 25);
        final int stepY = Math.max(100, h / 25);

        final GeoCoding geoCoding = product.getGeoCoding();
        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        final List<Point> geoBoundary = new ArrayList<Point>();

        for (int i = minY; i < maxY; i += stepY) {
            p.setLocation(minX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                geoBoundary.add(new Point(GeoPos.normalizeLon(g.getLon()), g.getLat()));
            }
        }
        for (int i = minX; i < maxX; i += stepX) {
            p.setLocation(i + 0.5, maxY + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                geoBoundary.add(new Point(GeoPos.normalizeLon(g.getLon()), g.getLat()));
            }
        }
        for (int i = maxY; i > minY; i -= stepY) {
            p.setLocation(maxX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                geoBoundary.add(new Point(GeoPos.normalizeLon(g.getLon()), g.getLat()));
            }
        }
        for (int i = maxX; i > minX; i -= stepX) {
            p.setLocation(i + 0.5, minY + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                geoBoundary.add(new Point(GeoPos.normalizeLon(g.getLon()), g.getLat()));
            }
        }
        if (geoBoundary.size() < 3) {
            throw new IllegalArgumentException(
                    String.format("only %d points in polygon of product %s", geoBoundary.size(), product.getName()));
        }
        geoBoundary.add(geoBoundary.get(0));
        if (PgUtil.isClockwise(geoBoundary)) {
            Collections.reverse(geoBoundary);
        }

        return createGeometry(geoBoundary.toArray(new Point[geoBoundary.size()]));
    }

    /**
     * Returns the pixel boundary of a product. The pixel boundary shall enclose only
     * pixels where the geo-location is valid.
     *
     * @param product The product.
     *
     * @return the pixel boundary of the product supplied as argument.
     *
     * @throws Exception when the pixel boundary cannot not be calculated.
     */
    private Rectangle getPixelBoundary(Product product) throws Exception {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            throw new Exception("Unable to get geo-coding for product '" + product.getName() + "'.");
        }
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        int minX = getMinX(product);
        int maxX = getMaxX(product);
        int minY = 0;
        int maxY = h - 1;
        if (minX == -1 || maxX == -1) {
            // no pair of opposing geo-coordinates at the horizontal boundaries is valid, try vertical boundaries
            minX = 0;
            maxX = w - 1;
            minY = getMinY(product);
            maxY = getMaxY(product);
            if (minY == -1 || maxY == -1) {
                // no pair of opposing geo-coordinates at the vertical boundaries is valid
                throw new Exception("Unable to get pixel-boundary for product '" + product.getName() + "'.");
            }
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static PGgeometry createGeometry(Point[] points) {
        return new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(points)}));
    }

    private static int getMinX(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = 0; i < w; i++) {
            p.setLocation(i + 0.5, 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(i + 0.5, h - 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int getMaxX(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = w; i-- > 0;) {
            p.setLocation(i + 0.5, 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(i + 0.5, h - 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int getMinY(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = 0; i < h; i++) {
            p.setLocation(0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(w - 0.5, i + 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int getMaxY(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = h; i-- > 0;) {
            p.setLocation(0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(w - 0.5, i + 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
