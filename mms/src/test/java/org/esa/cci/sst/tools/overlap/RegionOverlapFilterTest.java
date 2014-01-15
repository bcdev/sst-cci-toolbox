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
        final SamplingPoint samplingPoint = new SamplingPoint(19, 83);
        sampleList.add(samplingPoint);

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(1, filteredList.size());
        assertEquals(19, filteredList.get(0).getX());
        assertEquals(83, filteredList.get(0).getY());
    }

    @Test
    public void testFilter_twoPoints_nonOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        sampleList.add(new SamplingPoint(19, 83));
        sampleList.add(new SamplingPoint(36, 12));

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(2, filteredList.size());
    }

    @Test
    public void testFilter_twoPoints_overlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        sampleList.add(new SamplingPoint(19, 83));
        sampleList.add(new SamplingPoint(21, 86));

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(1, filteredList.size());
    }

    @Test
    public void testFilter_threePoints_nonOverlapping() {
        final List<SamplingPoint> sampleList = new ArrayList<>();
        sampleList.add(new SamplingPoint(19, 83));
        sampleList.add(new SamplingPoint(36, 12));
        sampleList.add(new SamplingPoint(102, 13));

        final List<SamplingPoint> filteredList = filter.filterOverlaps(sampleList);
        assertNotNull(filteredList);
        assertEquals(3, filteredList.size());
    }


}
