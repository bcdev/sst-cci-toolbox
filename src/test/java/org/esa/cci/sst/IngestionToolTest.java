package org.esa.cci.sst;

import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static junit.framework.Assert.*;

public class IngestionToolTest {
    @Test
    public void testCommandLineArgs() throws IngestionTool.ToolException {
        IngestionTool noArgs = new IngestionTool();
        assertTrue(noArgs.setCommandLineArgs(new String[]{}));
        assertEquals(false, noArgs.isDebug());
        assertEquals(false, noArgs.isVerbose());
        assertEquals("[auto-detect]", noArgs.getSchemaName());
        assertNotNull(noArgs.getInputFiles());
        assertEquals(0, noArgs.getInputFiles().length);

        IngestionTool onlyFiles = new IngestionTool();
        assertTrue(onlyFiles.setCommandLineArgs(new String[]{"file1", "file2", "file3"}));
        assertEquals(false, onlyFiles.isDebug());
        assertEquals(false, onlyFiles.isVerbose());
        assertEquals("[auto-detect]", noArgs.getSchemaName());
        assertNotNull(onlyFiles.getInputFiles());
        assertEquals(3, onlyFiles.getInputFiles().length);
        assertEquals(new File("file1"), onlyFiles.getInputFiles()[0]);
        assertEquals(new File("file2"), onlyFiles.getInputFiles()[1]);
        assertEquals(new File("file3"), onlyFiles.getInputFiles()[2]);

        IngestionTool printHelp = new IngestionTool();
        assertFalse(printHelp.setCommandLineArgs(new String[]{"-help"}));

        IngestionTool someOptions = new IngestionTool();
        assertTrue(someOptions.setCommandLineArgs(new String[]{
                "-D", "mms.p1=true",
                "-D", "mms.p2=6",
                "-schema", "aatsr",
                "-debug",
                "mmfile"}));
        assertEquals(true, someOptions.isDebug());
        assertEquals(false, someOptions.isVerbose());
        assertEquals("aatsr", someOptions.getSchemaName());
        assertNotNull(someOptions.getInputFiles());
        assertEquals(1, someOptions.getInputFiles().length);
        assertEquals(new File("mmfile"), someOptions.getInputFiles()[0]);
        assertEquals("true", someOptions.getConfiguration().getProperty("mms.p1"));
        assertEquals("6", someOptions.getConfiguration().getProperty("mms.p2"));

        URL resource = getClass().getResource("test.properties");
        assertTrue(someOptions.setCommandLineArgs(new String[]{
                "-conf", new File(resource.getPath()).getPath(),
                "mmfile"}));
        assertNotNull(someOptions.getInputFiles());
        assertEquals(1, someOptions.getInputFiles().length);
        assertEquals(new File("mmfile"), someOptions.getInputFiles()[0]);
        assertEquals("value1", someOptions.getConfiguration().getProperty("mms.name1"));
        assertEquals("value2", someOptions.getConfiguration().getProperty("mms.name2"));
    }

    @Test
    public void testConfiguration() {
        IngestionTool ingestionTool = new IngestionTool();
        assertNotNull(ingestionTool.getConfiguration());
        assertEquals(null, ingestionTool.getConfiguration().getProperty("mms.someParam1"));

        // test that "mms" parameters are copied from system properties
        System.setProperty("mms.someParam", "someValue");
        ingestionTool = new IngestionTool();
        assertEquals("someValue", ingestionTool.getConfiguration().getProperty("mms.someParam"));

        // test that returned configuration is immutable
        ingestionTool.getConfiguration().setProperty("xyz", "2");
        assertEquals(null, ingestionTool.getConfiguration().getProperty("xyz"));
    }

    @Test
    public void testCreateReader() throws IngestionTool.ToolException, IOException {
        assertNotNull(IngestionTool.createReader("aatsr"));
        assertNotNull(IngestionTool.createReader("metop"));
        assertNotNull(IngestionTool.createReader("seviri"));

        try {
            IngestionTool.createReader("bogus-reader");
            fail("ToolException expected");
        } catch (IngestionTool.ToolException e) {
            // ok
        }
    }
}
