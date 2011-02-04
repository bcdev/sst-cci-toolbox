package org.esa.cci.sst.util;

import org.postgis.Point;

import java.util.List;

import static java.lang.Math.*;

public class PgUtil {

    private PgUtil() {
    }

    public static boolean isClockwise(List<Point> geoBoundary) {
        // points defining the upper left corner
        final Point p1 = geoBoundary.get(geoBoundary.size() - 2);
        final Point p2 = geoBoundary.get(0);
        final Point p3 = geoBoundary.get(1);

        final double[] a = toXYZ(p1);
        final double[] b = toXYZ(p2);
        final double[] c = toXYZ(p3);
        // difference vector a - b
        final double[] d = new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
        // difference vector c - b
        final double[] e = new double[]{c[0] - b[0], c[1] - b[1], c[2] - b[2]};
        // cross product of (a - b) and (c - b)
        final double[] f = new double[]{
                d[1] * e[2] - d[2] * e[1],
                d[2] * e[0] - d[0] * e[2],
                d[0] * e[1] - d[1] * e[0]
        };
        return f[0] * b[0] + f[1] * b[1] + f[2] * b[2] > 0;
    }

    private static double[] toXYZ(Point p) {
        final double u = toRadians(p.getX());
        final double v = toRadians(p.getY());
        final double w = cos(v);

        final double ax = cos(u) * w;
        final double ay = sin(u) * w;
        final double az = sin(v);

        return new double[]{ax, ay, az};
    }

}
