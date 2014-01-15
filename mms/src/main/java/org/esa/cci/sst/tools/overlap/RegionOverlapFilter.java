package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.List;

public class RegionOverlapFilter {

    private final int width;
    private final int height;

    public RegionOverlapFilter(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public List<SamplingPoint> filterOverlaps(List<SamplingPoint> sampleList) {
        final ArrayList<SamplingPoint> filteredList = new ArrayList<>();
        if (sampleList.size() <= 1) {
            filteredList.addAll(sampleList);
            return filteredList;
        }

        final ArrayList<SamplingPoint> intermediateList = new ArrayList<>(sampleList);
        while (intermediateList.size() > 1) {
            final SamplingPoint p0 = intermediateList.get(0);
            intermediateList.remove(0);

            final ArrayList<SamplingPoint> intersections = getAllThatIntersect(p0, intermediateList);
            if (intersections.size() > 0) {
                // @todo 1 tb/tb continue here 2014-01-14
                // 1) search for intersections with all points in the first intersection list - to collect all points in a
                // clustered area - should end up in one area connecting all intersection areas connected to the search point p0
                // 2) thin out intersecting points until no intersections left.
                // 2a) calculate center of gravity for all points,
                // 2b) remove point closest to COG and repeat this until no intersections left
                // 3) add the remaining points (including p0) to the filtered list
                // 4) remove all points used in this operation from the intermediateList
                intermediateList.remove(intersections);
            } else {
                filteredList.add(p0);
            }
        }

        if (!intermediateList.isEmpty()) {
            filteredList.add(intermediateList.get(0));
        }

        return filteredList;
    }

    private ArrayList<SamplingPoint> getAllThatIntersect(SamplingPoint p0, ArrayList<SamplingPoint> intermediateList) {
        final ArrayList<SamplingPoint> intersections = new ArrayList<>(8);

        for (SamplingPoint p1 : intermediateList) {
            if (intersect(p0, p1)) {
                intersections.add(p1);
            }
        }

        return intersections;
    }

    private boolean intersect(SamplingPoint p1, SamplingPoint p2) {
        return Math.abs(p1.getX() - p2.getX()) <= width && Math.abs(p1.getY() - p2.getY()) <= height;
    }
}
