package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.List;

class IntersectionCalculator {

    private final int width;
    private final int height;

    IntersectionCalculator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    boolean intersect(SamplingPoint p1, SamplingPoint p2) {
        return Math.abs(p1.getX() - p2.getX()) < width && Math.abs(p1.getY() - p2.getY()) < height;
    }

    ArrayList<SamplingPoint> getAllThatIntersect(SamplingPoint p0, List<SamplingPoint> intermediateList) {
        final ArrayList<SamplingPoint> intersections = new ArrayList<>(8);

        for (SamplingPoint p1 : intermediateList) {
            if (intersect(p0, p1)) {
                intersections.add(p1);
            }
        }

        return intersections;
    }
}
