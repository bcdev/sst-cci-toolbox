package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

class OverlapCalculator {

    private final int width;
    private final int height;

    OverlapCalculator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    boolean areOverlapping(SamplingPoint p, SamplingPoint q) {
        return Math.abs(p.getX() - q.getX()) < width && Math.abs(p.getY() - q.getY()) < height;
    }
}
