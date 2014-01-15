package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

class IntersectionWrapper implements Comparable<IntersectionWrapper> {

    private SamplingPoint point;
    private int numIntersections;

    public SamplingPoint getPoint() {
        return point;
    }

    public void setPoint(SamplingPoint point) {
        this.point = point;
    }

    public int getNumIntersections() {
        return numIntersections;
    }

    public void setNumIntersections(int numIntersections) {
        this.numIntersections = numIntersections;
    }

    @Override
    public int compareTo(IntersectionWrapper other) {
        return other.getNumIntersections() - numIntersections;
    }
}
