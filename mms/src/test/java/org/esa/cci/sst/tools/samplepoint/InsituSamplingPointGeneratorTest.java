package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.orm.Storage;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class InsituSamplingPointGeneratorTest {

    private static File archiveDir;
    private static Sensor sensor;
    private InsituSamplePointGenerator generator;
    private Storage mockStorage;

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
        mockStorage = mock(Storage.class);
        generator = new InsituSamplePointGenerator(archiveDir, sensor, mockStorage);
    }

    @Test
    public void testGenerate_timeRangeBeforeData() throws URISyntaxException, ParseException {
        final long startTime = parseTime("1983-06-01T00:00:00Z");
        final long stopTime = parseTime("1983-06-04T00:00:00Z");

        final List<SamplingPoint> inSituPoints = generator.generate(startTime, stopTime);
        assertNotNull(inSituPoints);
        assertEquals(0, inSituPoints.size());

        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void testGenerate_timeRangeAfterData() throws URISyntaxException, ParseException {
        final long startTime = parseTime("2014-06-01T00:00:00Z");
        final long stopTime = parseTime("2014-06-04T00:00:00Z");

        final List<SamplingPoint> inSituPoints = generator.generate(startTime, stopTime);
        assertNotNull(inSituPoints);
        assertEquals(0, inSituPoints.size());

        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void testGenerate_timeRangeContainsOneFile() throws URISyntaxException, ParseException {
        final Date startDate = parseDate("2007-11-01T00:00:00Z");
        final Date stopDate = parseDate("2008-02-01T00:00:00Z");
        when(mockStorage.getDatafile(anyString())).thenReturn(null);

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(1285, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);

        assertNumDataFilesStored(1);
    }

    @Test
    public void testGenerate_timeRangeContainsOneFile_dataFileAlreadyStored() throws URISyntaxException, ParseException {
        final Date startDate = parseDate("2007-11-01T00:00:00Z");
        final Date stopDate = parseDate("2008-02-01T00:00:00Z");
        when(mockStorage.getDatafile(anyString())).thenReturn(new DataFile());

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(1285, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);

        verify(mockStorage, times(1)).getDatafile(anyString());
        verify(mockStorage, times(0)).store(any(DataFile.class));
        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void testGenerate_timeRangeContainsTwoFiles() throws URISyntaxException, ParseException {
        final Date startDate = parseDate("1984-06-01T00:00:00Z");
        final Date stopDate = parseDate("1985-02-30T00:00:00Z");
        when(mockStorage.getDatafile(anyString())).thenReturn(null);

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(35, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);

        assertNumDataFilesStored(2);
    }

    @Test
    public void testGenerate_timeRangeIntersectsOneFile() throws URISyntaxException, ParseException {
        final Date startDate = parseDate("2013-04-12T00:00:00Z");
        final Date stopDate = parseDate("2013-08-22T00:00:00Z");
        when(mockStorage.getDatafile(anyString())).thenReturn(null);

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(321, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);

        assertNumDataFilesStored(1);
    }

    @Test
    public void testGenerate_timeRangeIntersectsAllFiles() throws URISyntaxException, ParseException {
        final Date startDate = parseDate("1983-01-01T00:00:00Z");
        final Date stopDate = parseDate("2014-01-12T00:00:00Z");
        when(mockStorage.getDatafile(anyString())).thenReturn(null);

        final List<SamplingPoint> inSituPoints = generator.generate(startDate.getTime(), stopDate.getTime());
        assertNotNull(inSituPoints);
        assertEquals(38978, inSituPoints.size());

        TestHelper.assertPointsInTimeRange(startDate, stopDate, inSituPoints);

        assertNumDataFilesStored(10);
    }

    @Test
    public void testLogging() throws ParseException {
        final TestLogger testLogger = new TestLogger();

        generator.setLogger(testLogger);

        final Date startDate = parseDate("1998-01-01T00:00:00Z");
        final Date stopDate = parseDate("2012-01-12T00:00:00Z");
        generator.generate(startDate.getTime(), stopDate.getTime());

        assertEquals("Unparseable date: \"actual\"", testLogger.getWarning());
    }

    @Test
    public void testExtractTimeRangeFromFileName() throws ParseException {
        final TimeRange timeRange = InsituSamplePointGenerator.extractTimeRange("insitu_0_WMOID_71569_20030117_20030131.nc");
        assertNotNull(timeRange);

        assertEquals(parseTime("2003-01-17T00:00:00Z"), timeRange.getStartDate().getTime());
        assertEquals(parseTime("2003-01-31T23:59:59Z"), timeRange.getStopDate().getTime());
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

    @Test
    public void testCreateDataFile() {
        final File insituFile = new File("/yo/this/is/a/path/and_a_file.nc");

        final DataFile dataFile = InsituSamplePointGenerator.createDataFile(insituFile, sensor);
        assertNotNull(dataFile);

        assertEquals(insituFile.getPath(), dataFile.getPath());
        final Sensor sensor = dataFile.getSensor();
        assertNotNull(sensor);
        assertEquals("history", sensor.getName());
        assertEquals(4000000000000000L, sensor.getPattern());
    }

    private static long parseTime(String timeString) throws ParseException {
        return parseDate(timeString).getTime();
    }

    private static Date parseDate(String timeString) throws ParseException {
        return TimeUtil.parseCcsdsUtcFormat(timeString);
    }

    private void assertNumDataFilesStored(int storedDateFiles) {
        verify(mockStorage, times(storedDateFiles)).getDatafile(anyString());
        verify(mockStorage, times(storedDateFiles)).store(any(DataFile.class));
        verifyNoMoreInteractions(mockStorage);
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
