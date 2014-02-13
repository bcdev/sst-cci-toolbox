package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.*;

public class InsituSamplePointGeneratorTest {

    private static File archiveDir;
    private InsituSamplePointGenerator generator;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL testArchiveUrl = InsituSamplePointGeneratorTest.class.getResource("../../reader/insitu_0_WMOID_71569_20030117_20030131.nc");
        final URI uri = testArchiveUrl.toURI();
        archiveDir = new File(uri).getParentFile();
        assertTrue(archiveDir.isDirectory());
    }

    @Before
    public void setUp() {
        generator = new InsituSamplePointGenerator(archiveDir);
    }

    @Test
    public void testGenerate_noTimeConstraints() throws URISyntaxException {
        final List<SamplingPoint> inSituPoints = generator.generate();
        assertNotNull(inSituPoints);
        assertEquals(223 + 3 + 1285, inSituPoints.size());
    }

    @Test
    public void testGenerate_timeRangeBeforeData() throws URISyntaxException, ParseException {
        final long startTime = TimeUtil.parseCcsdsUtcFormat("1984-06-01T00:00:00Z").getTime();
        final long stopTime = TimeUtil.parseCcsdsUtcFormat("1984-06-04T00:00:00Z").getTime();

        final List<SamplingPoint> inSituPoints = generator.generate(startTime, stopTime);
        assertNotNull(inSituPoints);
        assertEquals(0, inSituPoints.size());
    }

    @Test
    public void testGenerate_timeRangeAfterData() throws URISyntaxException, ParseException {
        final long startTime = TimeUtil.parseCcsdsUtcFormat("2014-06-01T00:00:00Z").getTime();
        final long stopTime = TimeUtil.parseCcsdsUtcFormat("2014-06-04T00:00:00Z").getTime();

        final List<SamplingPoint> inSituPoints = generator.generate(startTime, stopTime);
        assertNotNull(inSituPoints);
        assertEquals(0, inSituPoints.size());
    }

    @Test
    public void testGenerate_timeRangeContainsOneFile() throws URISyntaxException, ParseException {
        final long startTime = TimeUtil.parseCcsdsUtcFormat("2007-11-01T00:00:00Z").getTime();
        final long stopTime = TimeUtil.parseCcsdsUtcFormat("2008-02-01T00:00:00Z").getTime();

        final List<SamplingPoint> inSituPoints = generator.generate(startTime, stopTime);
        assertNotNull(inSituPoints);
        assertEquals(1285, inSituPoints.size());
    }

    @Test
    public void testExtractTimeRangeFromFileName() throws ParseException {
        final TimeRange timeRange = InsituSamplePointGenerator.extractTimeRange("insitu_0_WMOID_71569_20030117_20030131.nc");
        assertNotNull(timeRange);

        assertEquals(TimeUtil.parseCcsdsUtcFormat("2003-01-17T00:00:00Z").getTime(), timeRange.getStartDate().getTime());
        assertEquals(TimeUtil.parseCcsdsUtcFormat("2003-01-31T23:59:59Z").getTime(), timeRange.getStopDate().getTime());
    }
}
