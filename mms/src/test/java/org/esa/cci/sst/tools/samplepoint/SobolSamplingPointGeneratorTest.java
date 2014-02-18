package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SobolSamplingPointGeneratorTest {

    @Test
    public void testGenerate() {
        final SobolSamplePointGenerator generator = new SobolSamplePointGenerator();

        final List<SamplingPoint> pointList = generator.createSamples(1000, 0, 0, 1000);
        assertNotNull(pointList);
        assertEquals(1000, pointList.size());

        for (SamplingPoint point : pointList) {
            final double lon = point.getLon();
            assertTrue(lon >= -180.0 && lon <= 180.0);

            final double lat = point.getLat();
            assertTrue(lat >= -90.0 && lat <= 90.0);

            final long time = point.getTime();
            assertTrue(time >= 0 && time <= 1000);
        }
    }
}
