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
    public void testConstructWith_lon_lat() {
        SamplingPoint point;

        point = new SamplingPoint(55.0, 75.0, 0, 0.0);

        assertEquals(55.0, point.getLon(), 0.0);
        assertEquals(75.0, point.getLat(), 0.0);

        point = new SamplingPoint(255.0, 31.0, 0, 0.0);

        assertEquals(255.0 - 360.0, point.getLon(), 0.0);
        assertEquals(31.0, point.getLat(), 0.0);
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
    public void testSetGetLon() {
        final double lon_1 = 54.8;
        final double lon_2 = -39.14;

        samplingPoint.setLon(lon_1);
        assertEquals(lon_1, samplingPoint.getLon(), 1e-8);

        samplingPoint.setLon(lon_2);
        assertEquals(lon_2, samplingPoint.getLon(), 1e-8);
    }

    @Test
    public void testSetGetLonWithNonNormalizedValues() {
        final double lon_1 = 254.80;
        final double lon_2 = 320.86;

        samplingPoint.setLon(lon_1);
        assertEquals(254.80 - 360.0, samplingPoint.getLon(), 1e-8);

        samplingPoint.setLon(lon_2);
        assertEquals(320.86 - 360.0, samplingPoint.getLon(), 1e-8);
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
    public void testSetGetReferenceLonWithNonNormalizedValues() {
        final double lon_1 = 254.80;
        final double lon_2 = 320.86;

        samplingPoint.setReferenceLon(lon_1);
        assertEquals(254.80 - 360.0, samplingPoint.getReferenceLon(), 1e-8);

        samplingPoint.setReferenceLon(lon_2);
        assertEquals(320.86 - 360.0, samplingPoint.getReferenceLon(), 1e-8);
    }

    @Test
    public void testSetGetReference2Time() {
         final long time_1 = 883457395L;
         final long time_2 = 6567645L;

        samplingPoint.setReference2Time(time_1);
        assertEquals(time_1, samplingPoint.getReference2Time());

        samplingPoint.setReference2Time(time_2);
        assertEquals(time_2, samplingPoint.getReference2Time());
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

    @Test
    public void testSetGetDatasetName() {
        final String name_1 = "Klaus";
        final String name_2 = "Sven-Jens";

        samplingPoint.setDatasetName(name_1);
        assertEquals(name_1, samplingPoint.getDatasetName());

        samplingPoint.setDatasetName(name_2);
        assertEquals(name_2, samplingPoint.getDatasetName());
    }
}
