package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.Collections;
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

            // 1) search for intersections with all points in the first intersection list - to collect all points in a
            // clustered area - should end up in one area connecting all intersection areas connected to the search point p0
            final List<SamplingPoint> clusterList = extractClusterContaining(p0, intermediateList);
            if (clusterList.size() > 0) {
                clusterList.add(p0);    // reference point also belongs to cluster

                // 2) thin out intersecting points until no intersections left.
                // 2a) calculate sum of intersecting regions for all points,
                // 2b) remove point with highest number of intersections and repeat this until no intersections left
                final List<SamplingPoint> nonIntersecionList = removeIntersecting(clusterList);

                // 3) add the remaining points (including p0) to the filtered list
                filteredList.addAll(nonIntersecionList);

                // 4) remove all points used in this operation from the intermediateList
                intermediateList.remove(clusterList);
            } else {
                filteredList.add(p0);
            }
        }

        if (!intermediateList.isEmpty()) {
            filteredList.add(intermediateList.get(0));
        }

        return filteredList;
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> removeIntersecting(List<SamplingPoint> clusterList) {
        if (clusterList.size() == 1) {
            return clusterList;
        } else {
            // copy to wrapper list
            final ArrayList<IntersectionWrapper> intersectionWrappers = new ArrayList<>(clusterList.size());
            for (SamplingPoint aClusterList : clusterList) {
                final IntersectionWrapper wrapper = new IntersectionWrapper();
                wrapper.setPoint(aClusterList);
                intersectionWrappers.add(wrapper);
            }
            // calculate intersection counts
            int sumIntersections = Integer.MAX_VALUE;
            while (sumIntersections > 0) {
                sumIntersections = 0;
                for (IntersectionWrapper wrapper : intersectionWrappers) {
                    final int intersections = getAllThatIntersect(wrapper.getPoint(), clusterList).size() - 1;   // remove self intersection
                    wrapper.setNumIntersections(intersections);
                    sumIntersections += intersections;
                }
                if (sumIntersections > 0) {
                    Collections.sort(intersectionWrappers);
                    final IntersectionWrapper removed = intersectionWrappers.remove(0);
                    clusterList.remove(removed.getPoint());
                }
            }
        }
        return clusterList;
    }

    // package access for testing only tb 2014-01-15
    boolean intersect(SamplingPoint p1, SamplingPoint p2) {
        return Math.abs(p1.getX() - p2.getX()) < width && Math.abs(p1.getY() - p2.getY()) < height;
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> extractClusterContaining(SamplingPoint samplingPoint, List<SamplingPoint> sampleList) {
        final ArrayList<SamplingPoint> clusterList = new ArrayList<>();

        final ArrayList<SamplingPoint> intersecting = getAllThatIntersect(samplingPoint, sampleList);
        if (intersecting.size() == 0) {
            return clusterList;
        }

        sampleList.removeAll(intersecting);
        while (intersecting.size() > 0) {
            final SamplingPoint intersectPoint = intersecting.remove(0);
            clusterList.add(intersectPoint);
            sampleList.remove(intersectPoint);

            final ArrayList<SamplingPoint> subIntersecting = getAllThatIntersect(intersectPoint, sampleList);
            intersecting.addAll(subIntersecting);
            sampleList.removeAll(subIntersecting);
        }
        return clusterList;
    }

    private ArrayList<SamplingPoint> getAllThatIntersect(SamplingPoint p0, List<SamplingPoint> intermediateList) {
        final ArrayList<SamplingPoint> intersections = new ArrayList<>(8);

        for (SamplingPoint p1 : intermediateList) {
            if (intersect(p0, p1)) {
                intersections.add(p1);
            }
        }

        return intersections;
    }
}
