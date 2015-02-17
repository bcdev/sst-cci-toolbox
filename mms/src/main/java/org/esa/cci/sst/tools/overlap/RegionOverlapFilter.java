package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class RegionOverlapFilter {

    private final OverlapCalculator overlapCalculator;
    private final Comparator<SamplingPoint> orderedComparator;

    public RegionOverlapFilter(int width, int height) {
        overlapCalculator = new OverlapCalculator(width, height);
        orderedComparator = new Comparator<SamplingPoint>() {
            @Override
            public final int compare(SamplingPoint o1, SamplingPoint o2) {
                final int compareY = Integer.compare(o1.getY(), o2.getY());
                if (compareY == 0) {
                    return Integer.compare(o1.getX(), o2.getX());
                } else {
                    return compareY;
                }
            }
        };
    }

    public List<SamplingPoint> apply(List<SamplingPoint> points) {
        final List<SamplingPoint> nonOverlappingPoints = new LinkedList<>();
        if (points.size() <= 1) {
            nonOverlappingPoints.addAll(points);
            return nonOverlappingPoints;
        }

        final List<List<SamplingPoint>> byOrbit = splitByOrbit(points);
        for (final List<SamplingPoint> pointsForOrbit : byOrbit) {
            nonOverlappingPoints.addAll(getNonOverlappingPoints(pointsForOrbit));
        }

        return nonOverlappingPoints;
    }

    private Collection<SamplingPoint> getNonOverlappingPoints(Collection<SamplingPoint> points) {
        final NavigableSet<SamplingPoint> unique = new TreeSet<>(orderedComparator);
        final NavigableSet<SamplingPoint> result = new TreeSet<>(orderedComparator);

        unique.addAll(points);
        for (final SamplingPoint point : unique) {
            if (overlapCalculator.areOverlapping(point, result)) {
                result.add(point);
            }
        }

        return result;
    }

    // package access for testing only tb 2014-01-17
    List<List<SamplingPoint>> splitByOrbit(List<SamplingPoint> points) {
        final Map<Integer, List<SamplingPoint>> map = new HashMap<>();
        for (final SamplingPoint point : points) {
            final int id = point.getReference();
            if (!map.containsKey(id)) {
                map.put(id, new ArrayList<SamplingPoint>());
            }
            map.get(id).add(point);
        }
        final ArrayList<List<SamplingPoint>> byOrbit = new ArrayList<>(map.size());
        for (final List<SamplingPoint> orbitPoints : map.values()) {
            byOrbit.add(orbitPoints);
        }

        return byOrbit;
    }
}
