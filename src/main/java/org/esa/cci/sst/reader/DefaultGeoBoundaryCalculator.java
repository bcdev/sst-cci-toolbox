package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.util.PgUtil;
import org.postgis.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultGeoBoundaryCalculator implements GeoBoundaryCalculator {

    @Override
    public Point[] getGeoBoundary(Product product) throws IOException {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            throw new IOException("Unable to get geo-coding for product '" + product.getName() + "'.");
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
                throw new IOException("Unable to get geo-boundary for product '" + product.getName() + "'.");
            }
        }
        final int stepX = Math.max(1, w / 25);
        final int stepY = Math.max(1, h / 25);

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        final List<Point> geoBoundary = new ArrayList<Point>();
        for (int i = minY; i < maxY; i += stepY) {
            p.setLocation(minX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = minX; i < maxX; i += stepX) {
            p.setLocation(i + 0.5, maxY + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = maxY; i > minY; i -= stepY) {
            p.setLocation(maxX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = maxX; i > minX; i -= stepX) {
            p.setLocation(i + 0.5, minY + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        geoBoundary.add(geoBoundary.get(0));
        if (PgUtil.isClockwise(geoBoundary)) {
            Collections.reverse(geoBoundary);
        }

        return geoBoundary.toArray(new Point[geoBoundary.size()]);
    }

    private int getMinX(Product product) {
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

    private int getMaxX(Product product) {
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

    private int getMinY(Product product) {
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

    private int getMaxY(Product product) {
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
