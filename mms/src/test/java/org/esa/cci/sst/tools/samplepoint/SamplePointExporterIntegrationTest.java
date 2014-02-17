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
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, test_archive.getAbsolutePath());
        config.put(Configuration.KEY_ARCHIVE_USECASE, "useCase");
        config.put(Configuration.KEY_MMS_SAMPLING_SENSOR, "schnickschnack.4");

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
}
