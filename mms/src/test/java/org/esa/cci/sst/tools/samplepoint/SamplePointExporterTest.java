package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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

    @Test
    public void testCreateOutputPath() {
         final String archiveRoot = "/archive";

        // # mms/archive/mms2/smp/atsr.3/2003/atsr.3-smp-2003-01-b.json
        String path = SamplePointExporter.createOutputPath(archiveRoot, "atsr.2", 2008, 5, 'a');
        assertEquals("/archive/smp/atsr.2/2008/atsr.2-smp-2008-05-a.json", path);

        path = SamplePointExporter.createOutputPath(archiveRoot, "atsr.3", 2010, 11, 'b');
        assertEquals("/archive/smp/atsr.3/2010/atsr.3-smp-2010-11-b.json", path);
    }

    @Test
    public void testGetArchiveRootPath() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, "/archive");
        config.put(Configuration.KEY_ARCHIVE_USECASE, "mms2");

        final String path = SamplePointExporter.getArchiveRootPath(config);
        assertNotNull(path);
        assertEquals("/archive" + File.separatorChar + "mms2", path);
    }

    @Test
    public void testGetArchiveRootPath_missingUseCase() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, "/archive");

        final String path = SamplePointExporter.getArchiveRootPath(config);
        assertNotNull(path);
        assertEquals("/archive", path);
    }

    @Test
    public void testGetArchiveRootPath_missingRoot() {
        final Configuration config = new Configuration();

        try {
            SamplePointExporter.getArchiveRootPath(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    private List<SamplingPoint> createSamplingPoints() throws ParseException {
        final long intervalStart = TimeUtil.parseCcsdsUtcFormat("2003-07-17T00:00:00Z").getTime();
        final long intervalStop = TimeUtil.parseCcsdsUtcFormat("2003-09-15T23:59:59Z").getTime();
        final SobolSamplePointGenerator generator = new SobolSamplePointGenerator();
        return generator.createSamples(200, 0, intervalStart, intervalStop);
    }
}
