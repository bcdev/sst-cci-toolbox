package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SamplePointExporterTest {

    @Test
    public void testExtractByTimeRange_intersectingBeginning() throws ParseException {
        final List<SamplingPoint> samples = createSamplingPoints();

        final Date extractStart = TimeUtil.parseCcsdsUtcFormat("2003-07-01T00:00:00Z");
        final Date extractStop = TimeUtil.parseCcsdsUtcFormat("2003-07-31T23:59:59Z");
        final TimeRange extractRange = new TimeRange(extractStart, extractStop);

        final List<SamplingPoint> julySamples = SamplePointExporter.extractSamples(samples, extractRange);
        assertNotNull(julySamples);
        assertEquals(49, julySamples.size());
        assertEquals(151, samples.size());
        TestHelper.assertPointsInTimeRange(extractStart, extractStop, julySamples);
    }

    @Test
    public void testExtractByTimeRange_intersectingMiddle() throws ParseException {
        final List<SamplingPoint> samples = createSamplingPoints();

        final Date extractStart = TimeUtil.parseCcsdsUtcFormat("2003-08-01T00:00:00Z");
        final Date extractStop = TimeUtil.parseCcsdsUtcFormat("2003-08-31T23:59:59Z");
        final TimeRange extractRange = new TimeRange(extractStart, extractStop);

        final List<SamplingPoint> augustSamples = SamplePointExporter.extractSamples(samples, extractRange);
        assertNotNull(augustSamples);
        assertEquals(102, augustSamples.size());
        assertEquals(98, samples.size());
        TestHelper.assertPointsInTimeRange(extractStart, extractStop, augustSamples);
    }

    @Test
    public void testExtractByTimeRange_intersectingEnd() throws ParseException {
        final List<SamplingPoint> samples = createSamplingPoints();

        final Date extractStart = TimeUtil.parseCcsdsUtcFormat("2003-09-01T00:00:00Z");
        final Date extractStop = TimeUtil.parseCcsdsUtcFormat("2003-09-30T23:59:59Z");
        final TimeRange extractRange = new TimeRange(extractStart, extractStop);

        final List<SamplingPoint> septemberSamples = SamplePointExporter.extractSamples(samples, extractRange);
        assertNotNull(septemberSamples);
        assertEquals(49, septemberSamples.size());
        assertEquals(151, samples.size());
        TestHelper.assertPointsInTimeRange(extractStart, extractStop, septemberSamples);
    }


    private List<SamplingPoint> createSamplingPoints() throws ParseException {
        final long intervalStart = TimeUtil.parseCcsdsUtcFormat("2003-07-17T00:00:00Z").getTime();
        final long intervalStop = TimeUtil.parseCcsdsUtcFormat("2003-09-15T23:59:59Z").getTime();
        final SobolSamplePointGenerator generator = new SobolSamplePointGenerator();
        final List<SamplingPoint> samples = generator.createSamples(200, 0, intervalStart, intervalStop);
        for (SamplingPoint samplingPoint : samples) {
            samplingPoint.setReferenceTime(samplingPoint.getTime());
        }
        return samples;
    }
}
