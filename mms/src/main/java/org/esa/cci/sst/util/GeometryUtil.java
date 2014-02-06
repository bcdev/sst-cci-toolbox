package org.esa.cci.sst.util;

public class GeometryUtil {

    public static double normalizeLongitude(double lon) {
        return (lon + 180.0 + 720.0) % 360.0 - 180.0;
    }
}
