package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.*;

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
        final List<List<SamplingPoint>> orbitLists = splitByOrbit(sampleList);
        // iterate over all lists
        for (List<SamplingPoint> orbit : orbitLists) {
            final List<SamplingPoint> filteredOrbitList = new LinkedList<>();
            filterSingleOrbitOverlaps(orbit, filteredOrbitList);

            filteredList.addAll(filteredOrbitList);
        }

        return filteredList;
    }

    private void filterSingleOrbitOverlaps(List<SamplingPoint> sampleList, List<SamplingPoint> filteredList) {
        final List<SamplingPoint> intermediateList = new LinkedList<>(sampleList);
        while (intermediateList.size() > 1) {
            final SamplingPoint p0 = intermediateList.get(0);
            intermediateList.remove(0);

            // 1) search for intersections with all points in the first intersection list - to collect all points in a
            // clustered area - should end up in one area connecting all intersection areas belonging to the search point p0
            final List<SamplingPoint> clusterList = extractClusterContaining(p0, intermediateList);
            if (clusterList.size() > 0) {
                clusterList.add(p0);    // reference point also belongs to cluster

                // 2) thin out intersecting points until no intersections left.
                // 2a) calculate sum of intersecting regions for all points,
                // 2b) remove point with highest number of intersections and repeat this until no intersections left
                final List<SamplingPoint> nonIntersectionList = removeIntersecting(clusterList);

                // 3) add the remaining points (including p0) to the filtered list
                filteredList.addAll(nonIntersectionList);

                // 4) remove all points used in this operation from the intermediateList
                intermediateList.removeAll(clusterList);
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
            final List<IntersectionWrapper> intersectionWrappers = wrapList(clusterList);

            // calculate intersection counts
            int sumIntersections = Integer.MAX_VALUE;
            while (sumIntersections > 0) {
                sumIntersections = 0;
                for (IntersectionWrapper wrapper : intersectionWrappers) {
                    final int intersections = ic.getAllIntersectingPoints(wrapper.getPoint(), clusterList).size() - 1;   // remove self intersection
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

    // package access for testing only tb 2014-01-17
    List<List<SamplingPoint>> splitByOrbit(List<SamplingPoint> pointList) {
        final List<List<SamplingPoint>> orbitLists = new ArrayList<>();
        if (pointList.size() > 0) {
            while (pointList.size() > 0) {
                final SamplingPoint samplingPoint = pointList.get(0);
                final List<SamplingPoint> allFromOrbit = extractAllFromOrbit(samplingPoint.getReference(), pointList);
                if (allFromOrbit.size() > 0) {
                    orbitLists.add(allFromOrbit);
                }
            }
        }
        return orbitLists;
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> extractClusterContaining(SamplingPoint samplingPoint, List<SamplingPoint> samples) {
        final List<SamplingPoint> clusterPoints = new LinkedList<>();

        final List<SamplingPoint> intersectingPoints = ic.getAllIntersectingPoints(samplingPoint, samples);
        if (intersectingPoints.size() == 0) {
            return clusterPoints;
        }

        samples.removeAll(intersectingPoints);
        while (intersectingPoints.size() > 0) {
            final SamplingPoint intersectPoint = intersectingPoints.remove(0);
            clusterPoints.add(intersectPoint);
            samples.remove(intersectPoint);

            final List<SamplingPoint> subIntersecting = ic.getAllIntersectingPoints(intersectPoint, samples);
            intersectingPoints.addAll(subIntersecting);
            samples.removeAll(subIntersecting);
        }
        return clusterPoints;
    }

    private List<SamplingPoint> extractAllFromOrbit(int orbitNo, List<SamplingPoint> pointList) {
        if (pointList instanceof LinkedList) {
            final LinkedList<SamplingPoint> extracted = new LinkedList<>();

            final Iterator<SamplingPoint> iterator = pointList.iterator();
            while (iterator.hasNext()) {
                final SamplingPoint point = iterator.next();
                if (point.getReference() == orbitNo) {
                    extracted.add(point);
                    iterator.remove();
                }
            }

            return extracted;
        } else if (pointList instanceof ArrayList) {
            final List<SamplingPoint> extracted = new LinkedList<>();
            final List<SamplingPoint> remaining = new ArrayList<>(pointList.size());

            for (final SamplingPoint point : pointList) {
                if (point.getReference() == orbitNo) {
                    extracted.add(point);
                } else {
                    remaining.add(point);
                }
            }
            pointList.clear();
            pointList.addAll(remaining);

            return extracted;
        } else {
            throw new IllegalArgumentException("Point list is neither a LinkedList nor an ArrayList.");
        }
    }

    private ArrayList<IntersectionWrapper> wrapList(List<SamplingPoint> clusterList) {
        final ArrayList<IntersectionWrapper> intersectionWrappers = new ArrayList<>(clusterList.size());
        for (SamplingPoint samplingPoint : clusterList) {
            final IntersectionWrapper wrapper = new IntersectionWrapper();
            wrapper.setPoint(samplingPoint);
            intersectionWrappers.add(wrapper);
        }
        return intersectionWrappers;
    }
}
