package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.Iterator;
import java.util.NavigableSet;

final class OverlapCalculator {

    private final int width;
    private final int height;

    OverlapCalculator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    boolean areOverlapping(SamplingPoint p, SamplingPoint q) {
        return Math.abs(p.getX() - q.getX()) < width && Math.abs(p.getY() - q.getY()) < height;
    }

    boolean areOverlapping(SamplingPoint point, NavigableSet<SamplingPoint> others) {
        for (Iterator<SamplingPoint> iterator = others.descendingIterator(); iterator.hasNext(); ) {
            final SamplingPoint other = iterator.next();
            if (other.getY() < point.getY() - height) {
                break;
            }
            if (other.getY() > point.getY() + height) {
                continue;
            }
            if (areOverlapping(point, other)) {
                return false;
            }
        }

        return true;
    }
}
