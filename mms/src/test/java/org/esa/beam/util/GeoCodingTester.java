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

package org.esa.beam.util;

import org.esa.beam.dataio.amsr2.Amsr2ProductReaderPlugIn;
import org.esa.beam.dataio.cci.sst.NcAvhrrGacProductReaderPlugIn;
import org.esa.beam.dataio.metop.MetopReaderPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.math.DistanceMeasure;
import org.esa.beam.util.math.SphericalDistance;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Ralf Quast
 */
public class GeoCodingTester {

    private static final double EPS = 2.0E-8;

    private final String formatName;
    private final double accuracy;

    public GeoCodingTester(String formatName, double accuracy) {
        this.formatName = formatName;
        this.accuracy = accuracy;
    }

    public static void main(String[] args) throws IOException {
        final String type = args[0];
        final String path = args[1];
        final GeoCodingTester tester = create(type);
        final int failureCount = tester.test(path);

        System.out.println("failureCount = " + failureCount);

        if (failureCount != 0) {
            System.exit(failureCount);
        }
    }

    private static GeoCodingTester create(String type) {
        switch (type) {
            case "amsr2":
                return new GeoCodingTester(Amsr2ProductReaderPlugIn.FORMAT_NAME, 0.0);
            case "avhrr":
                return new GeoCodingTester(NcAvhrrGacProductReaderPlugIn.FORMAT_NAME, 0.1);
            case "avhrr_f":
                return new GeoCodingTester(MetopReaderPlugIn.FORMAT_NAME, 0.02);
            default:
                throw new IllegalArgumentException(MessageFormat.format("No configuration for type ''{0}''.", type));
        }
    }

    private int test(String path) throws IOException {
        final ProductReader reader = ProductIO.getProductReader(formatName);
        final Product product = reader.readProductNodes(path, null);
        final GeoCoding geoCoding = product.getGeoCoding();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        int failureCount = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                p.setLocation(x + 0.5, y + 0.5);
                geoCoding.getGeoPos(p, g);

                if (g.isValid()) {
                    final double expectedLat = g.getLat();
                    final double expectedLon = g.getLon();

                    geoCoding.getPixelPos(g, p);

                    if (p.isValid()) {
                        final double actualX = Math.floor(p.getX());
                        final double actualY = Math.floor(p.getY());

                        final double dx = Math.abs(x - actualX);
                        final double dy = Math.abs(y - actualY);

                        if (dx > 0.0 || dy > 0.0) {
                            p.setLocation(actualX + 0.5, actualY + 0.5);
                            geoCoding.getGeoPos(p, g);

                            final double actualLat = g.getLat();
                            final double actualLon = g.getLon();
                            final DistanceMeasure distanceMeasure = new SphericalDistance(expectedLon, expectedLat);
                            final double d = distanceMeasure.distance(actualLon, actualLat);

                            if (dy < h / 2) {
                                if (d > EPS) {
                                    failureCount++;
                                }
                            } else { // failure is probably due to self-overlapping
                                if (d > Math.toRadians(accuracy)) {
                                    failureCount++;
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("y = " + y + ", failureCount = " + failureCount);
        }

        return failureCount;
    }
}
