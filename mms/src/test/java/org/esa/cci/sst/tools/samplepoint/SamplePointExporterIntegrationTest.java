package org.esa.cci.sst.tools.samplepoint;


import org.esa.beam.util.io.FileUtils;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
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
        final SobolSamplePointGenerator pointGenerator = new SobolSamplePointGenerator();
        final List<SamplingPoint> samples = pointGenerator.createSamples(200, 5678, startDate.getTime(), stopDate.getTime());

        final SamplePointExporter exporter = new SamplePointExporter(config);
        exporter.export(samples, new TimeRange(startDate, stopDate));

        final File targetDir = new File(test_archive, "useCase/smp/schnickschnack.4/2011");
        assertTrue(targetDir.isDirectory());

        final File mayFile = new File(targetDir, "schnickschnack.4-smp-2011-05-a.json");
        assertTrue(mayFile.isFile());
        assertEquals(8595, mayFile.length());

        final File juneFile = new File(targetDir, "schnickschnack.4-smp-2011-06-b.json");
        assertTrue(juneFile.isFile());
        assertEquals(13381, juneFile.length());

        final File julyFile = new File(targetDir, "schnickschnack.4-smp-2011-07-c.json");
        assertTrue(julyFile.isFile());
        assertEquals(7141, julyFile.length());
    }

    @Test
    public void testExportPointLists_withChangingYear() throws ParseException, IOException {
        final Configuration config = createConfig();

        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-12-13T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2011-02-17T00:00:00Z");
        final SobolSamplePointGenerator pointGenerator = new SobolSamplePointGenerator();
        final List<SamplingPoint> samples = pointGenerator.createSamples(200, 5678, startDate.getTime(), stopDate.getTime());

        final SamplePointExporter exporter = new SamplePointExporter(config);
        exporter.export(samples, new TimeRange(startDate, stopDate));

        final File targetDir2010 = new File(test_archive, "useCase/smp/schnickschnack.4/2010");
        assertTrue(targetDir2010.isDirectory());

        final File targetDir2011 = new File(test_archive, "useCase/smp/schnickschnack.4/2011");
        assertTrue(targetDir2011.isDirectory());

        final File decemberFile = new File(targetDir2010, "schnickschnack.4-smp-2010-12-a.json");
        assertTrue(decemberFile.isFile());
        assertEquals(8451, decemberFile.length());

        final File januaryFile = new File(targetDir2011, "schnickschnack.4-smp-2011-01-b.json");
        assertTrue(januaryFile.isFile());
        assertEquals(13668, januaryFile.length());

        final File februaryFile = new File(targetDir2011, "schnickschnack.4-smp-2011-02-c.json");
        assertTrue(februaryFile.isFile());
        assertEquals(6998, februaryFile.length());
    }

    private Configuration createConfig() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, test_archive.getAbsolutePath());
        config.put(Configuration.KEY_ARCHIVE_USECASE, "useCase");
        config.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "schnickschnack.4");
        return config;
    }
}
