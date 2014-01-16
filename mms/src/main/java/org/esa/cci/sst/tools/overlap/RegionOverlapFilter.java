package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RegionOverlapFilter {

    private final IntersectionCalculator ic;

    public RegionOverlapFilter(int width, int height) {
        ic = new IntersectionCalculator(width, height);
    }

    public List<SamplingPoint> filterOverlaps(List<SamplingPoint> sampleList) {
        final List<SamplingPoint> filteredList = new LinkedList<>();
        if (sampleList.size() <= 1) {
            filteredList.addAll(sampleList);
            return filteredList;
        }

        // split input into lists of same orbit reference
        // iterate over all lists
        filterSingleOrbitOverlaps(sampleList, filteredList);

        // merge filtered lists

        return filteredList;
    }

    private void filterSingleOrbitOverlaps(List<SamplingPoint> sampleList, List<SamplingPoint> filteredList) {
        final List<SamplingPoint> intermediateList = new LinkedList<>(sampleList);
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
                    final int intersections = ic.getAllThatIntersect(wrapper.getPoint(), clusterList).size() - 1;   // remove self intersection
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
    List<SamplingPoint> extractClusterContaining(SamplingPoint samplingPoint, List<SamplingPoint> sampleList) {
        final List<SamplingPoint> clusterList = new LinkedList<>();

        final List<SamplingPoint> intersecting = ic.getAllThatIntersect(samplingPoint, sampleList);
        if (intersecting.size() == 0) {
            return clusterList;
        }

        sampleList.removeAll(intersecting);
        while (intersecting.size() > 0) {
            final SamplingPoint intersectPoint = intersecting.remove(0);
            clusterList.add(intersectPoint);
            sampleList.remove(intersectPoint);

            final ArrayList<SamplingPoint> subIntersecting = ic.getAllThatIntersect(intersectPoint, sampleList);
            intersecting.addAll(subIntersecting);
            sampleList.removeAll(subIntersecting);
        }
        return clusterList;
    }
}
