package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RegionOverlapFilter {

    private static final Comparator<SamplingPoint> BY_Y_POINT_COMPARATOR = new Comparator<SamplingPoint>() {
        @Override
        public int compare(SamplingPoint o1, SamplingPoint o2) {
            return Integer.compare(o1.getY(), o2.getY());
        }
    };

    private final OverlapCalculator overlapCalculator;

    public RegionOverlapFilter(int width, int height) {
        overlapCalculator = new OverlapCalculator(width, height);
    }

    public List<SamplingPoint> makeSparse(List<SamplingPoint> points) {
        final List<SamplingPoint> sparsePoints = new LinkedList<>();
        if (points.size() <= 1) {
            sparsePoints.addAll(points);
            return sparsePoints;
        }

        final List<List<SamplingPoint>> byOrbit = splitByOrbit(points);
        for (final List<SamplingPoint> pointsByOrbit : byOrbit) {
            final List<SamplingPoint> nonOverlappingPoints = new LinkedList<>();
            removeSingleOrbitOverlaps(pointsByOrbit, nonOverlappingPoints);

            sparsePoints.addAll(nonOverlappingPoints);
        }

        return sparsePoints;
    }

    private void removeSingleOrbitOverlaps(List<SamplingPoint> pointsByOrbit, List<SamplingPoint> nonOverlappingPoints) {
        final List<SamplingPoint> intermediateList = new LinkedList<>(pointsByOrbit);

        while (intermediateList.size() > 1) {
            final SamplingPoint p0 = intermediateList.get(0);
            intermediateList.remove(0);

            // 1) search for intersections with all points in the first intersection list - to collect all points in a
            // clustered area - should end up in one area connecting all intersection areas belonging to the search point p0
            final List<SamplingPoint> clusteredPoints = extractClusteredPoints(p0, intermediateList);
            if (clusteredPoints.size() > 0) {
                clusteredPoints.add(p0);    // reference point also belongs to cluster

                // 2) thin out intersecting points until no intersections left.
                // 2a) calculate sum of intersecting regions for all points,
                // 2b) remove point with highest number of intersections and repeat this until no intersections left
                final List<SamplingPoint> sparsePoints = removeOverlappingPoints(clusteredPoints);

                // 3) add the remaining points (including p0) to the filtered list
                nonOverlappingPoints.addAll(sparsePoints);

                // 4) remove all points used in this operation from the intermediateList
                intermediateList.removeAll(clusteredPoints);
            } else {
                nonOverlappingPoints.add(p0);
            }
        }

        if (!intermediateList.isEmpty()) {
            nonOverlappingPoints.add(intermediateList.get(0));
        }
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> removeOverlappingPoints(List<SamplingPoint> clusteredPoints) {
        if (clusteredPoints.size() == 1) {
            return clusteredPoints;
        } else {
            // copy to wrapper list
            final List<IntersectionWrapper> intersectionWrappers = wrapList(clusteredPoints);

            // calculate intersection counts
            int sumIntersections = Integer.MAX_VALUE;
            while (sumIntersections > 0) {
                sumIntersections = 0;
                for (IntersectionWrapper wrapper : intersectionWrappers) {
                    final int intersections = overlapCalculator.getOverlappingPoints(wrapper.getPoint(),
                                                                      clusteredPoints).size() - 1;   // remove self intersection
                    wrapper.setNumIntersections(intersections);
                    sumIntersections += intersections;
                }
                if (sumIntersections > 0) {
                    Collections.sort(intersectionWrappers);
                    final IntersectionWrapper removed = intersectionWrappers.remove(0);
                    clusteredPoints.remove(removed.getPoint());
                }
            }
        }
        return clusteredPoints;
    }

    // package access for testing only tb 2014-01-17
    List<List<SamplingPoint>> splitByOrbit(List<SamplingPoint> points) {
        final Map<Integer, List<SamplingPoint>> map = new HashMap<>();
        for (final SamplingPoint point : points) {
            final int id = point.getReference();
            if (!map.containsKey(id)) {
                map.put(id, new LinkedList<SamplingPoint>());
            }
            map.get(id).add(point);
        }
        final ArrayList<List<SamplingPoint>> byOrbit = new ArrayList<>(map.size());
        for (final List<SamplingPoint> orbitPoints : map.values()) {
            byOrbit.add(sortByY(orbitPoints));
        }

        return byOrbit;
    }

    // package access for testing only
    List<SamplingPoint> sortByY(List<SamplingPoint> orbitPoints) {
        Collections.sort(orbitPoints, BY_Y_POINT_COMPARATOR);
        return orbitPoints;
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> extractClusteredPoints(SamplingPoint p, List<SamplingPoint> points) {
        final List<SamplingPoint> clusteredPoints = new LinkedList<>();

        final List<SamplingPoint> primaryOverlappingPoints = overlapCalculator.getOverlappingPoints(p, points);
        if (primaryOverlappingPoints.size() == 0) {
            return clusteredPoints;
        }

        points.removeAll(primaryOverlappingPoints);
        while (primaryOverlappingPoints.size() > 0) {
            final SamplingPoint q = primaryOverlappingPoints.remove(0);
            clusteredPoints.add(q);
            points.remove(q);

            final List<SamplingPoint> secondaryOverlappingPoints = overlapCalculator.getOverlappingPoints(q, points);
            primaryOverlappingPoints.addAll(secondaryOverlappingPoints);
            points.removeAll(secondaryOverlappingPoints);
        }
        return clusteredPoints;
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
