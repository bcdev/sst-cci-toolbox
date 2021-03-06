package org.esa.cci.sst.tools.samplepoint;


import org.esa.beam.util.io.FileUtils;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class SamplePointExporterIntegrationTest {

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
    public void testExportPointLists() throws ParseException, IOException {
        final Configuration config = createConfig();

        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2011-05-13T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2011-07-17T00:00:00Z");
        final List<SamplingPoint> samples = createSamplingPoints(startDate, stopDate);

        final SamplePointExporter exporter = new SamplePointExporter(config);
        exporter.export(samples, new TimeRange(startDate, stopDate));

        final File targetDir = new File(test_archive, "useCase/smp/schnickschnack.4/2011");
        assertTrue(targetDir.isDirectory());

        final File mayFile = new File(targetDir, "schnickschnack.4-smp-2011-05-a.json");
        assertIsFileWithLength(mayFile, 17858);

        final File juneFile = new File(targetDir, "schnickschnack.4-smp-2011-06-b.json");
        assertIsFileWithLength(juneFile, 27825);

        final File julyFile = new File(targetDir, "schnickschnack.4-smp-2011-07-c.json");
        assertIsFileWithLength(julyFile, 14834);
    }

    @Test
    public void testExportPointLists_withChangingYear() throws ParseException, IOException {
        final Configuration config = createConfig();

        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-12-13T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2011-02-17T00:00:00Z");
        final List<SamplingPoint> samples = createSamplingPoints(startDate, stopDate);

        final SamplePointExporter exporter = new SamplePointExporter(config);
        exporter.export(samples, new TimeRange(startDate, stopDate));

        final File targetDir2010 = new File(test_archive, "useCase/smp/schnickschnack.4/2010");
        assertTrue(targetDir2010.isDirectory());

        final File targetDir2011 = new File(test_archive, "useCase/smp/schnickschnack.4/2011");
        assertTrue(targetDir2011.isDirectory());

        final File decemberFile = new File(targetDir2010, "schnickschnack.4-smp-2010-12-a.json");
        assertIsFileWithLength(decemberFile, 17557);

        final File januaryFile = new File(targetDir2011, "schnickschnack.4-smp-2011-01-b.json");
        assertIsFileWithLength(januaryFile, 28426);

        final File februaryFile = new File(targetDir2011, "schnickschnack.4-smp-2011-02-c.json");
        assertIsFileWithLength(februaryFile, 14534);
    }

    @Test
    public void testExportPointLists_writesEmptyFiles() throws ParseException, IOException {
        final Configuration config = createConfig();

        final Date pointDate = TimeUtil.parseCcsdsUtcFormat("2008-09-16T00:00:00Z");
        final SamplingPoint samplingPoint = new SamplingPoint(-29.4, 11.8, 0, 0.56);
        samplingPoint.setReferenceTime(pointDate.getTime());
        final List<SamplingPoint> samples = new ArrayList<>();
        samples.add(samplingPoint);

        final SamplePointExporter exporter = new SamplePointExporter(config);
        exporter.export(samples, new TimeRange(pointDate, pointDate));

        final File targetDir = new File(test_archive, "useCase/smp/schnickschnack.4/2008");
        assertTrue(targetDir.isDirectory());

        final File augustFile = new File(targetDir, "schnickschnack.4-smp-2008-08-a.json");
        assertIsFileWithLength(augustFile, 21);

        final File septemberFile = new File(targetDir, "schnickschnack.4-smp-2008-09-b.json");
        assertIsFileWithLength(septemberFile, 283);

        final File octoberFile = new File(targetDir, "schnickschnack.4-smp-2008-10-c.json");
        assertIsFileWithLength(octoberFile, 21);
    }

    private void assertIsFileWithLength(File februaryFile, long lengthInBytes) {
        assertTrue(februaryFile.isFile());
        assertEquals(lengthInBytes, februaryFile.length());
    }

    private List<SamplingPoint> createSamplingPoints(Date startDate, Date stopDate) {
        final SobolSamplePointGenerator pointGenerator = new SobolSamplePointGenerator();
        final List<SamplingPoint> samples = pointGenerator.createSamples(200, 5678, startDate.getTime(), stopDate.getTime());
        for (SamplingPoint samplingPoint : samples) {
            samplingPoint.setReferenceTime(samplingPoint.getTime());
        }
        return samples;
    }

    private Configuration createConfig() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_ARCHIVE_ROOT, test_archive.getAbsolutePath());
        config.put(Configuration.KEY_MMS_USECASE, "useCase");
        config.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "schnickschnack.4");
        return config;
    }
}
