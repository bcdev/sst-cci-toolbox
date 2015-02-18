package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.beam.util.io.FileUtils;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tools.ArchiveUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(IoTestRunner.class)
public class MmdToolIOTest {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");

    private File archive_root;

    @Before
    public void setUp() {
        archive_root = new File("archive_root");
        createDirectory(archive_root);
    }

    @After
    public void tearDown() {
        if (archive_root.isDirectory()) {
            if (!FileUtils.deleteTree(archive_root)) {
                fail("unable to delete test directory");
            }
        }
    }

    @Test
    public void testGlobMatchingInputFiles_singleSensor_selectInputType() throws IOException {
        createFileInDir("clean", "atsr.3", 2008, 6);
        createFileInDir("clean-env", "atsr.3", 2008, 6);

        Configuration config = createConfiguration("clean", 2008, 6);
        File[] files = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3");
        assertEquals(1, files.length);
        assertEquals("atsr.3-clean-2008-06.json", files[0].getName());

        config = createConfiguration("clean-env", 2008, 6);
        files = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3");
        assertEquals(1, files.length);
        assertEquals("atsr.3-clean-env-2008-06.json", files[0].getName());

        config = createConfiguration("weired", 2008, 6);
        files = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3");
        assertEquals(0, files.length);
    }

    @Test
    public void testGlobMatchingInputFiles_singleSensor_selectSensor() throws IOException {
        createFileInDir("clean", "atsr.3", 2008, 6);
        createFileInDir("clean", "atsr.4", 2008, 6);

        Configuration config = createConfiguration("clean", 2008, 6);
        File[] files = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.4");
        assertEquals(1, files.length);
        assertEquals("atsr.4-clean-2008-06.json", files[0].getName());
    }

    @Test
    public void testGlobMatchingInputFiles_singleSensor_selectYear() throws IOException {
        createFileInDir("clean", "atsr.3", 2008, 6);
        createFileInDir("clean", "atsr.3", 2009, 7);
        createFileInDir("clean", "atsr.3", 2010, 8);

        Configuration config = createConfiguration("clean", 2009, 7);
        File[] files = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3");
        assertEquals(1, files.length);
        assertEquals("atsr.3-clean-2009-07.json", files[0].getName());
    }

    @Test
    public void testGlobMatchingInputFiles_dualSensor_1() throws Exception {
        createFileInDir("clean", "atsr.3,avhrr.n18", 2003, 6);
        createFileInDir("clean", "atsr.3,avhrr.n17", 2003, 6);
        createFileInDir("clean", "avhrr.n18,avhrr.n17", 2003, 6);

        final Configuration config = createConfiguration("clean", 2003, 6);
        File[] result;

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3");
        assertEquals(2, result.length);

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "avhrr.n17");
        assertEquals(2, result.length);

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "avhrr.n18");
        assertEquals(2, result.length);
    }

    @Test
    public void testGlobMatchingInputFiles_dualSensor_2() throws Exception {
        createFileInDir("clean", "atsr.3,avhrr.n18", 2003, 6);
        createFileInDir("clean", "atsr.3,avhrr.n17", 2003, 6);
        createFileInDir("clean", "avhrr.n18,avhrr.n17", 2003, 6);

        final Configuration config = createConfiguration("clean", 2003, 6);
        File[] result;

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "atsr.3,avhrr.n17");
        assertEquals(1, result.length);

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "avhrr.n17,avhrr.n18");
        assertEquals(0, result.length);

        result = MmdTool.globMatchingInputFiles(config, archive_root.getAbsolutePath(), "avhrr.n18,avhrr.n17");
        assertEquals(1, result.length);
    }

    private void createFileInDir(String type, String sensorName, int year, int month) throws IOException {
        final String filePath = ArchiveUtils.createTypedPath(archive_root.getAbsolutePath(), new String[]{sensorName}, year, month, type);
        final File file = new File(filePath);
        final File parent = file.getParentFile();
        createDirectory(parent);
        createFile(parent, file.getName());
    }

    private Configuration createConfiguration(String inputType, int year, int month) {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_MMD_INPUT_TYPE, inputType);
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_START_TIME, Integer.toString(year) + "-" + monthFormat.format(month) + "-01T00:00:00Z");
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_STOP_TIME, Integer.toString(year) + "-" + monthFormat.format(month) + "-31T00:00:00Z");
        return configuration;
    }

    private void createFile(File subDirectory, String fileName) throws IOException {
        final File file = new File(subDirectory, fileName);
        if (!file.createNewFile()) {
            fail("unable to create test file: " + file);
        }
    }

   private void createDirectory(File subDir) {
        if (!subDir.mkdirs()) {
            fail("unable to create test directory: " + subDir);
        }
    }
}
