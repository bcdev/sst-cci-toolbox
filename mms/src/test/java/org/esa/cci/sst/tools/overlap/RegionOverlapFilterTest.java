package org.esa.cci.sst.tools.overlap;


import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RegionOverlapFilterTest {

    private RegionOverlapFilter filter;

    @Before
    public void setUp() {
        filter = new RegionOverlapFilter(7, 7);
    }

    @Test
    public void testFilter_emptyInput() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        assertEquals(0, sampleList.size());

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(0, filteredList.size());
    }

    @Test
    public void testFilter_onePoint() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(1, filteredList.size());
        assertSamplePointAt(19, 83, 0, filteredList);
    }

    @Test
    public void testFilter_twoPoints_nonOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(36, 12, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());
    }

    @Test
    public void testFilter_twoPoints_overlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(21, 86, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(1, filteredList.size());
        assertSamplePointAt(19, 83, 0, filteredList);
    }

    @Test
    public void testFilter_threePoints_nonOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(36, 12, sampleList);
        addSamplePoint(102, 13, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(3, filteredList.size());
    }

    @Test
    public void testFilter_threePoints_twoOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(21, 82, sampleList);
        addSamplePoint(102, 13, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());

        assertSamplePointAt(19, 83, 0, filteredList);
        assertSamplePointAt(102, 13, 1, filteredList);
    }

    @Test
    public void testFilter_threePoints_overlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(100, 100, sampleList);
        addSamplePoint(105, 101, sampleList);
        addSamplePoint(106, 107, sampleList);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());

        assertSamplePointAt(106, 107, 0, filteredList);
        assertSamplePointAt(100, 100, 1, filteredList);
    }

    @Test
    public void testExtractCluster_empty() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        final SamplingPoint p0 = new SamplingPoint(3, 4);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(0, clusterList.size());
        assertEquals(0, sampleList.size());
    }

    @Test
    public void testExtractCluster_noClusterInList() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(23, 78, sampleList);
        addSamplePoint(54, 209, sampleList);
        addSamplePoint(502, 32076, sampleList);
        final SamplingPoint p0 = new SamplingPoint(100, 100);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(0, clusterList.size());
        assertEquals(3, sampleList.size());
    }

    @Test
    public void testExtractCluster_onePointCluster() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(23, 78, sampleList);
        addSamplePoint(102, 98, sampleList);
        addSamplePoint(502, 32076, sampleList);
        final SamplingPoint p0 = new SamplingPoint(100, 100);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(1, clusterList.size());
        assertEquals(2, sampleList.size());
    }

    @Test
    public void testExtractCluster_twoPointsCluster() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(23, 78, sampleList);
        addSamplePoint(102, 98, sampleList);
        addSamplePoint(98, 98, sampleList);
        final SamplingPoint p0 = new SamplingPoint(100, 100);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(2, clusterList.size());
        assertEquals(1, sampleList.size());
    }

    @Test
    public void testExtractCluster_twoPointsCluster_oneNotIntersectingSearchPoint() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(104, 97, sampleList);
        addSamplePoint(3457, 7753, sampleList);
        addSamplePoint(107, 98, sampleList);
        final SamplingPoint p0 = new SamplingPoint(100, 100);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(2, clusterList.size());
        assertEquals(1, sampleList.size());
    }

    @Test
    public void testExtractCluster_chainOfPoints() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(106, 102, sampleList);
        addSamplePoint(3457, 7753, sampleList);
        addSamplePoint(112, 103, sampleList);
        addSamplePoint(2456, 7753, sampleList);
        addSamplePoint(115, 109, sampleList);
        addSamplePoint(3001, 10087, sampleList);
        addSamplePoint(111, 114, sampleList);
        addSamplePoint(116, 114, sampleList);
        addSamplePoint(3101, 11087, sampleList);
        final SamplingPoint p0 = new SamplingPoint(100, 100);

        final List<SamplingPoint> clusterList = filter.extractClusterContaining(p0, sampleList);
        assertNotNull(clusterList);
        assertEquals(5, clusterList.size());
        assertEquals(4, sampleList.size());
    }

    @Test
    public void testRemoveIntersecting_empty() {
        final List<SamplingPoint> clusterList = new ArrayList<>();

        final List<SamplingPoint> thinnedOutList = filter.removeIntersecting(clusterList);
        assertNotNull(thinnedOutList);
        assertEquals(0, thinnedOutList.size());
    }

    @Test
    public void testRemoveIntersecting_onePoint() {
        final List<SamplingPoint> clusterList = new ArrayList<>();
        addSamplePoint(111, 114, clusterList);

        final List<SamplingPoint> thinnedOutList = filter.removeIntersecting(clusterList);
        assertNotNull(thinnedOutList);
        assertEquals(1, thinnedOutList.size());
    }

    @Test
    public void testRemoveIntersecting_twoPoints() {
        final List<SamplingPoint> clusterList = new ArrayList<>();
        addSamplePoint(111, 114, clusterList);
        addSamplePoint(113, 110, clusterList);

        final List<SamplingPoint> thinnedOutList = filter.removeIntersecting(clusterList);
        assertNotNull(thinnedOutList);
        assertEquals(1, thinnedOutList.size());
    }

    @Test
    public void testSplitByOrbit_emptyList() {
        final List<SamplingPoint> pointList = new ArrayList<>();

        final List<List<SamplingPoint>> orbitLists = filter.splitByOrbit(pointList);
        assertNotNull(orbitLists);
        assertEquals(0, orbitLists.size());
    }

    @Test
    public void testSplitByOrbit_onePoint() {
        final List<SamplingPoint> pointList = new ArrayList<>();
        addSamplePoint(34, pointList);

        final List<List<SamplingPoint>> orbitLists = filter.splitByOrbit(pointList);
        assertNotNull(orbitLists);
        assertEquals(1, orbitLists.size());

        final List<SamplingPoint> orbitPoints = orbitLists.get(0);
        assertNotNull(orbitPoints);
        assertEquals(1, orbitPoints.size());
    }

    @Test
    public void testSplitByOrbit_twoPoints_sameOrbit() {
        final List<SamplingPoint> pointList = new ArrayList<>();
        addSamplePoint(34, pointList);
        addSamplePoint(34, pointList);

        final List<List<SamplingPoint>> orbitLists = filter.splitByOrbit(pointList);
        assertNotNull(orbitLists);
        assertEquals(1, orbitLists.size());

        final List<SamplingPoint> orbitPoints = orbitLists.get(0);
        assertNotNull(orbitPoints);
        assertEquals(2, orbitPoints.size());
    }

    @Test
    public void testSplitByOrbit_fivePoints_twoOrbits() {
        final List<SamplingPoint> pointList = new ArrayList<>();
        addSamplePoint(34, pointList);
        addSamplePoint(34, pointList);
        addSamplePoint(34, pointList);
        addSamplePoint(67, pointList);
        addSamplePoint(67, pointList);

        final List<List<SamplingPoint>> orbitLists = filter.splitByOrbit(pointList);
        assertNotNull(orbitLists);
        assertEquals(2, orbitLists.size());

        List<SamplingPoint> orbitPoints = orbitLists.get(0);
        assertNotNull(orbitPoints);
        assertEquals(3, orbitPoints.size());

        orbitPoints = orbitLists.get(1);
        assertNotNull(orbitPoints);
        assertEquals(2, orbitPoints.size());
    }

    @Test
    public void testSplitByOrbit_twelvePoints_fourOrbits() {
        final List<SamplingPoint> pointList = new ArrayList<>();
        addSamplePoint(34, pointList);
        addSamplePoint(23, pointList);
        addSamplePoint(34, pointList);
        addSamplePoint(23, pointList);
        addSamplePoint(67, pointList);
        addSamplePoint(83, pointList);
        addSamplePoint(67, pointList);
        addSamplePoint(83, pointList);
        addSamplePoint(67, pointList);
        addSamplePoint(83, pointList);
        addSamplePoint(23, pointList);
        addSamplePoint(34, pointList);

        final List<List<SamplingPoint>> orbitLists = filter.splitByOrbit(pointList);
        assertNotNull(orbitLists);
        assertEquals(4, orbitLists.size());

        List<SamplingPoint> orbitPoints = orbitLists.get(0);
        assertNotNull(orbitPoints);
        assertEquals(3, orbitPoints.size());

        orbitPoints = orbitLists.get(1);
        assertNotNull(orbitPoints);
        assertEquals(3, orbitPoints.size());

        orbitPoints = orbitLists.get(2);
        assertNotNull(orbitPoints);
        assertEquals(3, orbitPoints.size());

        orbitPoints = orbitLists.get(3);
        assertNotNull(orbitPoints);
        assertEquals(3, orbitPoints.size());
    }

    private void assertSamplePointAt(int expectedX, int expectedY, int index, List<SamplingPoint> filteredList) {
        assertEquals(expectedX, filteredList.get(index).getX());
        assertEquals(expectedY, filteredList.get(index).getY());
    }

    private void addSamplePoint(int x, int y, List<SamplingPoint> sampleList) {
        sampleList.add(new SamplingPoint(x, y));
    }
    private void addSamplePoint(int orbitNo, List<SamplingPoint> sampleList) {
        final SamplingPoint point = new SamplingPoint(8743, 667);
        point.setReference(orbitNo);
        sampleList.add(point);
    }
}
