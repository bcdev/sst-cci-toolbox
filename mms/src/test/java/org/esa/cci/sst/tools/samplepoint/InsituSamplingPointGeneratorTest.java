package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class InsituSamplingPointGeneratorTest {

    private static File archiveDir;
    private static Sensor sensor;
    private InsituSamplePointGenerator generator;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL testArchiveUrl = InsituSamplingPointGeneratorTest.class.getResource("../../reader/insitu_0_WMOID_71566_20020211_20120214.nc");
        final URI uri = testArchiveUrl.toURI();
        archiveDir = new File(uri).getParentFile();
        assertTrue(archiveDir.isDirectory());

        sensor = new SensorBuilder().name("history").pattern(4000000000000000L).build();
    }

    @Before
    public void setUp() {
        generator = new InsituSamplePointGenerator(archiveDir, sensor);
    }

    @Test
    public void testGenerate_timeRangeBeforeData() throws URISyntaxException, ParseException {
        final long startTime = TimeUtil.parseCcsdsUtcFormat("1983-06-01T00:00:00Z").getTime();
        final long stopTime = TimeUtil.parseCcsdsUtcFormat("1983-06-04T00:00:00Z").getTime();

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
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2007-11-01T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2008-02-01T00:00:00Z");

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(1285, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);
    }

    @Test
    public void testGenerate_timeRangeContainsTwoFiles() throws URISyntaxException, ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2003-01-31T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2007-12-01T00:00:00Z");

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(207, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);
    }

    @Test
    public void testGenerate_timeRangeIntersectsOneFile() throws URISyntaxException, ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2003-01-01T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2003-01-12T00:00:00Z");

        // @todo 1 tb/tb reanimate when all data ist updated to latest in-situ format tb 2014-03-03
//        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
//        assertNotNull(inSituPoints);
//        assertEquals(1, inSituPoints.size());
//
//        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);
    }

    @Test
    public void testGenerate_timeRangeIntersectsAlFiles() throws URISyntaxException, ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("1998-01-01T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2012-01-12T00:00:00Z");
// @todo 1 tb/tb reanimate when all data ist updated to latest in-situ format tb 2014-03-03
//        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
//        assertNotNull(inSituPoints);
//        assertEquals(223 + 3 + 1285, inSituPoints.size());
//
//        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);
    }

    @Test
    public void testLogging() throws ParseException {
        final TestLogger testLogger = new TestLogger();

        generator.setLogger(testLogger);

        // @todo 1 tb/tb reanimate when all data ist updated to latest in-situ format tb 2014-03-03
//        final Date startDate = TimeUtil.parseCcsdsUtcFormat("1998-01-01T00:00:00Z");
//        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2012-01-12T00:00:00Z");
//        generator.generate(startDate.getTime(), stopDate.getTime());
//
//        assertEquals("Unparseable date: \"actual\"", testLogger.getWarning());
    }

    @Test
    public void testExtractTimeRangeFromFileName() throws ParseException {
        final TimeRange timeRange = InsituSamplePointGenerator.extractTimeRange("insitu_0_WMOID_71569_20030117_20030131.nc");
        assertNotNull(timeRange);

        assertEquals(TimeUtil.parseCcsdsUtcFormat("2003-01-17T00:00:00Z").getTime(), timeRange.getStartDate().getTime());
        assertEquals(TimeUtil.parseCcsdsUtcFormat("2003-01-31T23:59:59Z").getTime(), timeRange.getStopDate().getTime());
    }

    @Test
    public void testExtractTimeRangeFromFileName_unparseableDate() throws ParseException {
        try {
            InsituSamplePointGenerator.extractTimeRange("insitu_0_WMOID_71569_20030117_christmas.nc");
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testExtractTimeRangeFromFileName_wrongFileNamePattern() throws ParseException {
        try {
            InsituSamplePointGenerator.extractTimeRange("insitu.measurement.we.did.on.sunday.nc");
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    @Test
    public void testExtractTimeRangeFromFileName_notEnoughUnderscores() throws ParseException {
        try {
            InsituSamplePointGenerator.extractTimeRange("insitu.measurement_20120227.nc");
            fail("ParseException expected");
        } catch (ParseException expected) {
        }
    }

    private final class TestLogger extends Logger {

        private String warning;

        private TestLogger() {
            super("we", null);
        }

        @Override
        public void warning(String msg) {
            warning = msg;
        }

        String getWarning() {
            return warning;
        }
    }
}
