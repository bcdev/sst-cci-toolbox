package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;

import java.util.ArrayList;
import java.util.List;

public class SobolSamplePointGenerator {

    public List<SamplingPoint> createSamples(int sampleCount, int sampleSkip, long startTime, long stopTime) {
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(4);
        sequenceGenerator.skip(sampleSkip);
        final List<SamplingPoint> sampleList = new ArrayList<>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            final double[] sample = sequenceGenerator.nextVector();
            final double x = sample[0];
            final double y = sample[1];
            final double t = sample[2];
            final double random = sample[3];

            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;
            final long time = (long) (t * (stopTime - startTime)) + startTime;

            sampleList.add(new SamplingPoint(lon, lat, time, random));
        }

        return sampleList;
    }
}
