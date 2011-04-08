package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.Assert.*;

public class IngestionToolTest {

    @Test
    public void testCommandLineArgs() throws ToolException, URISyntaxException {
        IngestionTool noArgs = new IngestionTool();
        assertTrue(noArgs.setCommandLineArgs(new String[]{}));
        assertEquals(false, noArgs.isDebug());
        assertEquals(false, noArgs.isVerbose());
        if (new File(MmsTool.DEFAULT_CONFIGURATION_FILE_NAME).exists()) {
            assertNotNull(noArgs.getConfiguration().getProperty("openjpa.ConnectionURL"));
        }

        IngestionTool configOnly = new IngestionTool();
        final URL url = IngestionToolTest.class.getResource("ingestionToolTest.properties");
        final File configFile = new File(url.toURI());
        assertTrue(configOnly.setCommandLineArgs(new String[]{"-c", configFile.getPath()}));
        assertEquals("value1", configOnly.getConfiguration().getProperty("mms.name1"));
        if (new File(MmsTool.DEFAULT_CONFIGURATION_FILE_NAME).exists()) {
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
        assertNotNull(IOHandlerFactory.createHandler(Constants.DATA_SCHEMA_NAME_ATSR_MD, null));
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
