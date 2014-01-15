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
        assertSamplePointAt(21, 86, 0, filteredList);
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

        assertSamplePointAt(21, 82, 0, filteredList);
        assertSamplePointAt(102, 13, 1, filteredList);
    }

    private void assertSamplePointAt(int expectedX, int expectedY, int index, List<SamplingPoint> filteredList) {
        assertEquals(expectedX, filteredList.get(index).getX());
        assertEquals(expectedY, filteredList.get(index).getY());
    }

    private void addSamplePoint(int x, int y, List<SamplingPoint> sampleList) {
        sampleList.add(new SamplingPoint(x, y));
    }
}
