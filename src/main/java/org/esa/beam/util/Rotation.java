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

import static java.lang.Math.*;

/**
 * Class for rotating geographical positions.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class Rotation {

    private final double a11;
    private final double a12;
    private final double a13;
    private final double a21;
    private final double a22;
    private final double a23;
    private final double a31;
    private final double a32;
    private final double a33;

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lat, lon) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p/>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction) and a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction).
     *
     * @param lon the geographical longitude of the point becoming the new
     *            origin.
     * @param lat the geographical latitude of the point becoming the new
     *            origin.
     */
    Rotation(double lon, double lat) {
        this(lon, lat, 0.0);
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lat, lon) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p/>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction), a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction), and a counter
     * clockwise rotation about the x-axis by the angle alpha.
     *
     * @param lon   the geographical longitude of the point becoming the new
     *              origin.
     * @param lat   the geographical latitude of the point becoming the new
     *              origin.
     * @param alpha the rotation angle (degrees) corresponding to the x-axis.
     */
    Rotation(double lon, double lat, double alpha) {
        final double u = toRadians(lon);
        final double v = toRadians(lat);
        final double w = toRadians(alpha);

        final double cu = cos(u);
        final double cv = cos(v);
        final double cw = cos(w);
        final double su = sin(u);
        final double sv = sin(v);
        final double sw = sin(w);

        a11 = cu * cv;
        a12 = su * cv;
        a13 = sv;

        a21 = sw * (cu * sv) - su * cw;
        a22 = cw * cu + sw * (su * sv);
        a23 = -sw * cv;

        a31 = sw * -su - cw * (cu * sv);
        a32 = sw * cu - cw * (su * sv);
        a33 = cw * cv;
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lat, lon) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p/>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction) and a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction).
     *
     * @param point the point becoming the new origin, the x and y components
     *              of which define, respectively, the geographical longitude
     *              and latitude.
     */
    Rotation(Point2D point) {
        this(point, 0.0);
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lat, lon) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p/>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction), a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction), and a counter
     * clockwise rotation about the x-axis by the angle alpha.
     *
     * @param point the point becoming the new origin, the x and y components
     *              of which define, respectively, the geographical longitude
     *              and latitude.
     * @param alpha the rotation angle (degrees) corresponding to the x-axis.
     */
    Rotation(Point2D point, double alpha) {
        this(point.getX(), point.getY(), alpha);
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the center of the (lat, lon) parameters is located at the origin,
     * i.e. has the geographical coordinates (0, 0).
     * <p/>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction), and a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction).
     *
     * @param lons the longitude coordinates.
     * @param lats the latitude coordinates.
     */
    Rotation(double[] lons, double[] lats) {
        this(calculateCentralGeoPos(lons, lats));
    }

    /**
     * Transforms a given geographical point into the rotated coordinate
     * system.
     *
     * @param point the point.
     */
    void transform(Point2D point) {
        final double[] lon = new double[]{point.getX()};
        final double[] lat = new double[]{point.getY()};

        transform(lon, lat);
        point.setLocation(lon[0], lat[0]);
    }

    /**
     * Transforms a given set of geographical longitudes and latitudes
     * into the rotated coordinate system.
     *
     * @param lons the geographical longitudes.
     * @param lats the geographical latitudes.
     */
    void transform(double[] lons, double[] lats) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a12 * y + a13 * z;
            final double y2 = a21 * x + a22 * y + a23 * z;
            final double z2 = a31 * x + a32 * y + a33 * z;

            lats[i] = toDegrees(asin(z2));
            lons[i] = toDegrees(atan2(y2, x2));
        }
    }

    /**
     * Transforms a given geographical point back into the non-rotated
     * coordinate system.
     *
     * @param point the point.
     */
    void inverseTransform(Point2D point) {
        final double[] lon = new double[]{point.getX()};
        final double[] lat = new double[]{point.getY()};

        inverseTransform(lon, lat);
        point.setLocation(lon[0], lat[0]);
    }

    /**
     * Transforms a given set of geographical longitudes and latitudes
     * back into the non-rotated coordinate system.
     *
     * @param lons the geographical longitudes.
     * @param lats the geographical latitudes.
     */
    void inverseTransform(double[] lons, double[] lats) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a21 * y + a31 * z;
            final double y2 = a12 * x + a22 * y + a32 * z;
            final double z2 = a13 * x + a23 * y + a33 * z;

            lats[i] = toDegrees(asin(z2));
            lons[i] = toDegrees(atan2(y2, x2));
        }
    }

    private static Point2D calculateCentralGeoPos(double[] lons, double[] lats) {
        final int size = lats.length;
        final double[] x = new double[size];
        final double[] y = new double[size];
        final double[] z = new double[size];

        calculateXYZ(lons, lats, x, y, z);

        double xc = 0.0;
        double yc = 0.0;
        double zc = 0.0;
        for (int i = 0; i < size; i++) {
            if (isValid(x[i]) && isValid(y[i]) && isValid(z[i])) {
                xc += x[i];
                yc += y[i];
                zc += z[i];
            }
        }
        final double length = Math.sqrt(xc * xc + yc * yc + zc * zc);
        xc /= length;
        yc /= length;
        zc /= length;

        final double lat = toDegrees(asin(zc));
        final double lon = toDegrees(atan2(yc, xc));

        return new Point2D.Double(lat, lon);
    }

    private static void calculateXYZ(double[] lons, double[] lats, double[] x, double[] y, double[] z) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);
            final double w = cos(v);

            x[i] = cos(u) * w;
            y[i] = sin(u) * w;
            z[i] = sin(v);
        }
    }

    private static boolean isValid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
