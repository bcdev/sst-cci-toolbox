package org.esa.cci.sst.tools.overlap;

import org.esa.cci.sst.util.SamplingPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class RegionOverlapFilter {

    private final OverlapComparator overlapComparator;

    public RegionOverlapFilter(int width, int height) {
        overlapComparator = new OverlapComparator(new OverlapCalculator(width, height));
    }

    public List<SamplingPoint> apply(List<SamplingPoint> points) {
        final List<SamplingPoint> sparsePoints = new LinkedList<>();
        if (points.size() <= 1) {
            sparsePoints.addAll(points);
            return sparsePoints;
        }

        final List<List<SamplingPoint>> byOrbit = splitByOrbit(points);
        for (final List<SamplingPoint> pointsByOrbit : byOrbit) {
            sparsePoints.addAll(getSparsePoints(pointsByOrbit));
        }

        return sparsePoints;
    }

    private Collection<SamplingPoint> getSparsePoints(Collection<SamplingPoint> pointsByOrbit) {
        final Set<SamplingPoint> sparseSet = new TreeSet<>(overlapComparator);
        sparseSet.addAll(pointsByOrbit);

        return sparseSet;
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

    private static final class OverlapComparator implements Comparator<SamplingPoint> {

        private final OverlapCalculator overlapCalculator;

        public OverlapComparator(OverlapCalculator overlapCalculator) {
            this.overlapCalculator = overlapCalculator;
        }

        @Override
        public int compare(SamplingPoint o1, SamplingPoint o2) {
            if (overlapCalculator.areOverlapping(o1, o2)) {
                return 0;
            }
            final int compareY = Integer.compare(o1.getY(), o2.getY());
            if (compareY == 0) {
                return Integer.compare(o1.getX(), o2.getX());
            } else {
                return compareY;
            }
        }
    }
}
