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

import org.esa.beam.framework.datamodel.RationalFunctionModel;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * Helper class used by the {@link QuadTreePixelFinder} for storing its
 * result.
 *
 * @author Ralf Quast
 */
final class Result {

    private final double[] xs = new double[3];
    private final double[] ys = new double[3];
    private final double[] lons = new double[3];
    private final double[] lats = new double[3];

    private final Rotation rotation;
    private final double[] deltas = new double[3];

    static double delta(double lon, double lon0) {
        final double e = Math.abs(lon - lon0);
        return e < 180.0 ? e : 360.0 - e;
    }

    Result(double lon, double lat) {
        rotation = new Rotation(lon, lat);
        Arrays.fill(deltas, Double.POSITIVE_INFINITY);
    }

    boolean invalidate(double d0, double d1, double d2, double d3) {
        if (d0 < getDelta() || d1 < getDelta() || d2 < getDelta() || d3 < getDelta()) {
            final boolean b0 = Double.isNaN(d0);
            final boolean b1 = Double.isNaN(d1);
            final boolean b2 = Double.isNaN(d2);
            final boolean b3 = Double.isNaN(d3);

            // allow a single NaN value
            if (!b1 && !b2 && !b3 || !b0 && !b2 && !b3 || !b0 && !b1 && !b3 || !b0 && !b1 && !b2) {
                for (int i = 0; i < 3; i++) {
                    xs[i] = 0.0;
                    ys[i] = 0.0;
                    lons[i] = 0.0;
                    lats[i] = 0.0;
                    deltas[i] = Double.MAX_VALUE;
                }
                return true;
            }
        }
        return false;
    }

    void add(int x, int y, double lon, double lat, double delta) {
        for (int i = 0; i < 3; i++) {
            if (delta < deltas[i]) {
                for (int k = i + 1; k < 3; k++) {
                    xs[k] = xs[k - 1];
                    ys[k] = ys[k - 1];
                    lons[k] = lons[k - 1];
                    lats[k] = lats[k - 1];
                    deltas[k] = deltas[k - 1];
                }
                xs[i] = x + 0.5;
                ys[i] = y + 0.5;
                lons[i] = lon;
                lats[i] = lat;
                deltas[i] = delta;
                break;
            }
        }
    }

    void get(Point2D p) {
        rotation.transform(lons, lats);
        final RationalFunctionModel xModel = new RationalFunctionModel(1, 0, lons, lats, xs);
        final RationalFunctionModel yModel = new RationalFunctionModel(1, 0, lons, lats, ys);

        final double x = xModel.getValue(0.0, 0.0);
        final double y = yModel.getValue(0.0, 0.0);

        p.setLocation(x, y);
    }

    private double getDelta() {
        return deltas[0];
    }
}
