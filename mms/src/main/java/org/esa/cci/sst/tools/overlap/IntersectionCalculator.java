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

    boolean are_intersecting(SamplingPoint p1, SamplingPoint p2) {
        return Math.abs(p1.getX() - p2.getX()) < width && Math.abs(p1.getY() - p2.getY()) < height;
    }

    List<SamplingPoint> getAllIntersectingPoints(SamplingPoint p0, List<SamplingPoint> samplingPoints) {
        final List<SamplingPoint> intersectingPoints = new ArrayList<>(samplingPoints.size());

        for (SamplingPoint p1 : samplingPoints) {
            if (are_intersecting(p0, p1)) {
                intersectingPoints.add(p1);
            }
        }

        return intersectingPoints;
    }
}
