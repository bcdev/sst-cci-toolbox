package org.esa.cci.sst.util;


import org.esa.cci.sst.common.InsituDatasetId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SamplingPointTest {

    private SamplingPoint samplingPoint;

    @Before
    public void setUp() {
        samplingPoint = new SamplingPoint();
    }

    @Test
    public void testConstructWith_x_y() {
        final SamplingPoint point = new SamplingPoint(16, 345);

        assertEquals(16, point.getX());
        assertEquals(345, point.getY());
    }

    @Test
    public void testSetGetReferenceTime() {
        final long time_1 = 672645872435L;
        final long time_2 = 888483745L;

        samplingPoint.setReferenceTime(time_1);
        assertEquals(time_1, samplingPoint.getReferenceTime());

        samplingPoint.setReferenceTime(time_2);
        assertEquals(time_2, samplingPoint.getReferenceTime());
    }

    @Test
    public void testSetGetReferenceLat() {
        final double lat_1 = 44.8;
        final double lat_2 = -29.14;

        samplingPoint.setReferenceLat(lat_1);
        assertEquals(lat_1, samplingPoint.getReferenceLat(), 1e-8);

        samplingPoint.setReferenceLat(lat_2);
        assertEquals(lat_2, samplingPoint.getReferenceLat(), 1e-8);
    }

    @Test
    public void testSetGetReferenceLon() {
        final double lon_1 = 54.8;
        final double lon_2 = -39.14;

        samplingPoint.setReferenceLon(lon_1);
        assertEquals(lon_1, samplingPoint.getReferenceLon(), 1e-8);

        samplingPoint.setReferenceLon(lon_2);
        assertEquals(lon_2, samplingPoint.getReferenceLon(), 1e-8);
    }

    @Test
    public void testIsInsitu() {
        final SamplingPoint sobolPoint = new SamplingPoint(1, 2, 3, 56);
        assertFalse(sobolPoint.isInsitu());

        final SamplingPoint insituPoint = new SamplingPoint(1, 2, 3, Double.NaN);
        assertTrue(insituPoint.isInsitu());
    }

    @Test
    public void testSetGetInsituDatasetId() {
        samplingPoint.setInsituDatasetId(InsituDatasetId.xbt);
        assertEquals(InsituDatasetId.xbt, samplingPoint.getInsituDatasetId());

        samplingPoint.setInsituDatasetId(InsituDatasetId.mooring);
        assertEquals(InsituDatasetId.mooring, samplingPoint.getInsituDatasetId());
    }
}
