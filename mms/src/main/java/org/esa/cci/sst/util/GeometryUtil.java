package org.esa.cci.sst.util;

import org.postgis.PGgeometry;
import org.postgis.Point;

public class GeometryUtil {

    public static double normalizeLongitude(double lon) {
        return (lon + 180.0 + 720.0) % 360.0 - 180.0;
    }

    public static PGgeometry createPointGeometry(double lon, double lat) {
        return new PGgeometry(new Point(lon, lat));
    }
}
