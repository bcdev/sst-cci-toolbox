package org.esa.cci.sst.tools.samplepoint;


import org.esa.beam.util.io.FileUtils;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SamplePointImporterIntegrationTest {

    private static final String USE_CASE = "mms3";
    private static final String SENSOR = "sensor.5";
    private static final String YEAR = "2007";

    private File test_archive;

    @Before
    public void setUp() {
        test_archive = new File("test_archive");
        if (!test_archive.mkdir()) {
            fail("unable to create test directory");
        }
    }

    @After
    public void tearDown() {
        if (test_archive != null) {
            if (!FileUtils.deleteTree(test_archive)) {
                fail("unable to delete test directory");
            }
        }
    }

    @Test
    public void testLoad_missingFile_triggersLogMessage() throws IOException {
        final Configuration config = createConfig();
        final TestLogger testLogger = new TestLogger();
        final SamplePointImporter importer = new SamplePointImporter(config);
        importer.setLogger(testLogger);

        writePoints(Collections.<SamplingPoint>emptyList(), SENSOR + "-smp-" + YEAR + "-01-b.json");
        writePoints(Collections.<SamplingPoint>emptyList(), SENSOR + "-smp-" + YEAR + "-01-c.json");

        config.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2007-01-01T00:00:00Z");
        config.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2007-01-31T23:59:59Z");

        final List<SamplingPoint> loadedPoints = importer.load();
        assertNotNull(loadedPoints);
        assertEquals(0, loadedPoints.size());

        final String warning = testLogger.getWarning();
        assertNotNull(warning);
        assertTrue(warning.contains("Missing input file:"));
        assertTrue(warning.contains("sensor.5-smp-2007-01-a.json"));
    }

    @Test
    public void testLoad_onePointInOneFile() throws ParseException, IOException {
        final Configuration config = createConfig();

        final SamplePointImporter importer = new SamplePointImporter(config);

        final ArrayList<SamplingPoint> listB = new ArrayList<>();
        final SamplingPoint pointWritten = new SamplingPoint(34.5, 65.4, TimeUtil.parseCcsdsUtcFormat("2007-01-14T11:16:23Z").getTime(), 0.4533);
        listB.add(pointWritten);

        writePoints(Collections.<SamplingPoint>emptyList(), SENSOR + "-smp-" + YEAR + "-01-a.json");
        writePoints(listB, SENSOR + "-smp-" + YEAR + "-01-b.json");
        writePoints(Collections.<SamplingPoint>emptyList(), SENSOR + "-smp-" + YEAR + "-01-c.json");

        final List<SamplingPoint> loadedPoints = importer.load();
        assertNotNull(loadedPoints);
        assertEquals(1, loadedPoints.size());

        final SamplingPoint pointLoaded = loadedPoints.get(0);
        assertEquals(pointWritten.getTime(), pointLoaded.getTime());
        assertEquals(pointWritten.getIndex(), pointLoaded.getIndex());
        assertEquals(pointWritten.getLat(), pointLoaded.getLat(), 1e-8);
        assertEquals(pointWritten.getLon(), pointLoaded.getLon(), 1e-8);
    }

    @Test
    public void testLoad_pointsInAllFiles() throws ParseException, IOException {
        final Configuration config = createConfig();

        final SamplePointImporter importer = new SamplePointImporter(config);

        final ArrayList<SamplingPoint> listA = new ArrayList<>();
        listA.add(new SamplingPoint(34.5, 65.4, TimeUtil.parseCcsdsUtcFormat("2007-01-14T11:16:23Z").getTime(), 0.4533));

        final ArrayList<SamplingPoint> listB = new ArrayList<>();
        listB.add(new SamplingPoint(34.5, 65.4, TimeUtil.parseCcsdsUtcFormat("2007-01-15T11:16:23Z").getTime(), 0.4533));

        final ArrayList<SamplingPoint> listC = new ArrayList<>();
        listC.add(new SamplingPoint(34.5, 65.4, TimeUtil.parseCcsdsUtcFormat("2007-01-16T11:16:23Z").getTime(), 0.4533));

        writePoints(listA, SENSOR + "-smp-" + YEAR + "-01-a.json");
        writePoints(listB, SENSOR + "-smp-" + YEAR + "-01-b.json");
        writePoints(listC, SENSOR + "-smp-" + YEAR + "-01-c.json");

        final List<SamplingPoint> loadedPoints = importer.load();
        assertNotNull(loadedPoints);
        assertEquals(3, loadedPoints.size());
    }

    private Configuration createConfig() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_ARCHIVE_ROOT, test_archive.getAbsolutePath());
        config.put(Configuration.KEY_MMS_USECASE, USE_CASE);
        config.put(Configuration.KEY_MMS_SAMPLING_SENSOR, SENSOR);
        config.put(Configuration.KEY_MMS_SAMPLING_START_TIME, "2007-01-01T00:00:00Z");
        config.put(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2007-01-31T23:59:59Z");
        return config;
    }

    private void writePoints(List<SamplingPoint> pointList, String filename) throws IOException {
        final File targetDir = new File(test_archive, "/" + USE_CASE + "/smp/" + SENSOR + "/" + YEAR);
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                fail("unable to create target directory: " + targetDir.getAbsolutePath());
            }
        }

        final File outputFile = new File(targetDir, filename);
        if (!outputFile.createNewFile()) {
            fail("unable to create target file: " + outputFile.getAbsolutePath());
        }

        final FileOutputStream outputStream = new FileOutputStream(outputFile);
        SamplingPointIO.write(pointList, outputStream);
        outputStream.close();
    }
}
