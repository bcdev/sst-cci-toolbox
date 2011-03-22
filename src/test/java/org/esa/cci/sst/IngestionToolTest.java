package org.esa.cci.sst;

import org.esa.cci.sst.reader.IOHandlerFactory;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static junit.framework.Assert.*;

public class IngestionToolTest {

    @Test
    public void testCommandLineArgs() throws ToolException, URISyntaxException {
        IngestionTool noArgs = new IngestionTool();
        assertTrue(noArgs.setCommandLineArgs(new String[]{}));
        assertEquals(false, noArgs.isDebug());
        assertEquals(false, noArgs.isVerbose());
        if (MmsTool.DEFAULT_CONFIGURATION_FILE.exists()) {
            assertNotNull(noArgs.getConfiguration().getProperty("openjpa.ConnectionURL"));
        }

        IngestionTool configOnly = new IngestionTool();
        final File configFile = new File(getClass().getResource("test.properties").toURI());
        assertTrue(configOnly.setCommandLineArgs(new String[]{"-c", configFile.getPath()}));
        assertEquals("value1", configOnly.getConfiguration().getProperty("mms.name1"));
        if (MmsTool.DEFAULT_CONFIGURATION_FILE.exists()) {
            assertNull(configOnly.getConfiguration().getProperty("openjpa.ConnectionURL"));
        }

        IngestionTool printHelp = new IngestionTool();
        assertFalse(printHelp.setCommandLineArgs(new String[]{"-help"}));
    }

    @Test
    public void testConfiguration() {
        System.setProperty("mms.someParam", "someValue");
        IngestionTool ingestionTool = new IngestionTool();
        assertEquals("someValue", ingestionTool.getConfiguration().getProperty("mms.someParam"));
    }

    @Test
    public void testCreateReader() throws Exception {
        assertNotNull(IOHandlerFactory.createHandler(Constants.DATA_SCHEMA_NAME_AATSR_MD, null));
        assertNotNull(IOHandlerFactory.createHandler(Constants.DATA_SCHEMA_NAME_METOP_MD, null));
        assertNotNull(IOHandlerFactory.createHandler(Constants.DATA_SCHEMA_NAME_SEVIRI_MD, null));

        try {
            IOHandlerFactory.createHandler("bogus-reader", null);
            fail("Exception expected");
        } catch (Exception expected) {
            // ok
        }
    }
}
