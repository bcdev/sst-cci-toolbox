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

    public List<SamplingPoint> apply(List<SamplingPoint> points) {
        final List<SamplingPoint> sparsePoints = new LinkedList<>();
        if (points.size() <= 1) {
            sparsePoints.addAll(points);
            return sparsePoints;
        }

        System.out.println("Splitting ...");
        final List<List<SamplingPoint>> byOrbit = splitByOrbit(points);
        for (final List<SamplingPoint> pointsByOrbit : byOrbit) {
            final List<SamplingPoint> nonOverlappingPoints = new LinkedList<>();
            removeSingleOrbitOverlaps(pointsByOrbit, nonOverlappingPoints);

            sparsePoints.addAll(nonOverlappingPoints);
        }

        return sparsePoints;
    }

    private void removeSingleOrbitOverlaps(List<SamplingPoint> pointsByOrbit,
                                           List<SamplingPoint> nonOverlappingPoints) {
        // making a copy of the list put in increases performance
        final List<SamplingPoint> copy = new LinkedList<>(pointsByOrbit);

        while (copy.size() > 1) {
            final SamplingPoint point = copy.get(0);
            copy.remove(0);

            // 1) search for intersections with all points in the first intersection list - to collect all points in a
            // clustered area - should end up in one area connecting all intersection areas belonging to the search point p0
            final List<SamplingPoint> clusteredPoints = extractClusteredPoints(copy, point);
            System.out.println("#clustered points = " + clusteredPoints.size());
            if (clusteredPoints.size() > 0) {
                // reference point also belongs to cluster
                clusteredPoints.add(point);

                // 2) remove overlapping points until overlaps do not occur
                // 2a) calculate sum of intersecting regions for all points,
                // 2b) remove point with highest number of intersections and repeat this until no intersections left
                final List<SamplingPoint> sparsePoints = makeSparse(clusteredPoints);
                System.out.println("#sparse clustered points = " + clusteredPoints.size());

                // 3) add the remaining points (including the reference point) to the list of non-overlapping points
                nonOverlappingPoints.addAll(sparsePoints);

                // 4) remove all points used in this operation from the list of points
                copy.removeAll(clusteredPoints);
            } else {
                nonOverlappingPoints.add(point);
            }
        }
        if (!copy.isEmpty()) {
            nonOverlappingPoints.add(copy.get(0));
        }
    }

    // package access for testing only tb 2014-01-15
    List<SamplingPoint> makeSparse(List<SamplingPoint> clusteredPoints) {
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
            System.out.println("Sorting ...");
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
    List<SamplingPoint> extractClusteredPoints(List<SamplingPoint> points, SamplingPoint p) {
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
