package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.LinkedList;
import java.util.List;

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

    List<SamplingPoint> getOverlappingPoints(SamplingPoint point, List<SamplingPoint> others) {
        final List<SamplingPoint> overlappingPoints = new LinkedList<>();

        for (final SamplingPoint other : others) {
            if (other.getY() > point.getY() + height) {
                break;
            }
            if (other.getY() < point.getY() - height) {
                continue;
            }
            if (areOverlapping(point, other)) {
                overlappingPoints.add(other);
            }
        }

        return overlappingPoints;
    }
}
