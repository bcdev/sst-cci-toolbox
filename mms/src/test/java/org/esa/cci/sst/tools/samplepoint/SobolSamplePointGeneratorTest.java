package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class SobolSamplePointGeneratorTest {

    @Test
    public void testGenerate() {
        final SobolSamplePointGenerator generator = new SobolSamplePointGenerator();

        final List<SamplingPoint> pointList = generator.createSamples(12, 34, 89756L, 8756L);
        assertNotNull(pointList);
    }
}
