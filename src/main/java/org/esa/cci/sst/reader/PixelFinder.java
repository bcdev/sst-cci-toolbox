package org.esa.cci.sst.reader;

import java.awt.geom.Point2D;

public interface PixelFinder {

    boolean findPixel(double lon, double lat, Point2D pixelPos);
}
