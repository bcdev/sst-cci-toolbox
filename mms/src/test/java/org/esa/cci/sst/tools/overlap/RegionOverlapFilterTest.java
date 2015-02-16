package org.esa.cci.sst.tools.overlap;


import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
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

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(0, filteredList.size());
    }

    @Test
    public void testFilter_onePoint() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(1, filteredList.size());
        assertSamplePointAt(19, 83, 0, filteredList);
    }

    @Test
    public void testFilter_twoPoints_nonOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(36, 12, sampleList);

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());
    }

    @Test
    public void testFilter_twoPoints_overlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(21, 86, sampleList);

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
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

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(3, filteredList.size());
    }

    @Test
    public void testFilter_threePoints_twoOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(19, 83, sampleList);
        addSamplePoint(21, 82, sampleList);
        addSamplePoint(102, 13, sampleList);

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());

        assertSamplePointAt(102, 13, 0, filteredList);
        assertSamplePointAt(19, 83, 1, filteredList);
    }

    @Test
    public void testFilter_threePoints_overlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        addSamplePoint(100, 100, sampleList);
        addSamplePoint(105, 101, sampleList);
        addSamplePoint(106, 107, sampleList);

        final List<SamplingPoint> filteredList = filter.apply(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());

        assertSamplePointAt(100, 100, 0, filteredList);
        assertSamplePointAt(106, 107, 1, filteredList);
    }

    @Test
    public void testRemoveIntersecting_empty() {
        final List<SamplingPoint> clusterList = new ArrayList<>();

        final List<SamplingPoint> thinnedOutList = filter.apply(clusterList);
        assertNotNull(thinnedOutList);
        assertEquals(0, thinnedOutList.size());
    }

    @Test
    public void testRemoveIntersecting_onePoint() {
        final List<SamplingPoint> clusterList = new ArrayList<>();
        addSamplePoint(111, 114, clusterList);

        final List<SamplingPoint> thinnedOutList = filter.apply(clusterList);
        assertNotNull(thinnedOutList);
        assertEquals(1, thinnedOutList.size());
    }

    @Test
    public void testRemoveIntersecting_twoPoints() {
        final List<SamplingPoint> clusterList = new ArrayList<>();
        addSamplePoint(111, 114, clusterList);
        addSamplePoint(113, 110, clusterList);

        final List<SamplingPoint> thinnedOutList = filter.apply(clusterList);
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

    @Test
    @Ignore
    public void testTheBigList() {
        final int width = 512;
        final int height = 40000;
        final int maxOrbit = 1;
        final int count = 150000;
        final LinkedList<SamplingPoint> samplingPoints = new LinkedList<>();
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(3);

        for (int i = 0; i < count; i++) {
            final double[] rands = sequenceGenerator.nextVector();
            final int x = (int) (width * rands[0]);
            final int y = (int) (height * rands[1]);
            final int orbitRef = (int) (maxOrbit * rands[2]);
            final SamplingPoint point = new SamplingPoint(x, y);
            point.setReference(orbitRef);
            samplingPoints.add(point);
        }
        final Timer timer = new Timer();
        System.out.println("Initial count  = " + samplingPoints.size());
        timer.start();

        final List<SamplingPoint> filtererList = filter.apply(samplingPoints);

        timer.stop();
        System.out.println("Filtered count = " + filtererList.size());
        System.out.println("time [s]       = " + timer.deltaTInSecs());
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

    private class Timer {

        private long startTime;
        private long stopTime;

        void start() {
            startTime = System.currentTimeMillis();
        }

        void stop() {
            stopTime = System.currentTimeMillis();
        }

        double deltaTInSecs() {
            return (stopTime - startTime) / 1000.0;
        }
    }
}
