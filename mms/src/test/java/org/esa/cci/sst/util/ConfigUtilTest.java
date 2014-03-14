package org.esa.cci.sst.util;


import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ConfigUtilTest {

    @Test
    public void testGetUsecaseRootPath() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_ARCHIVE_ROOT, "/archive");
        config.put(Configuration.KEY_MMS_USECASE, "mms2");

        final String path = ConfigUtil.getUsecaseRootPath(config);
        assertNotNull(path);
        assertEquals("/archive" + File.separatorChar + "mms2", path);
    }

    @Test
    public void testGetUsecaseRootPath_missingUseCase() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_ARCHIVE_ROOT, "/archive");

        final String path = ConfigUtil.getUsecaseRootPath(config);
        assertNotNull(path);
        assertEquals("/archive", path);
    }

    @Test
    public void testGetUsecaseRootPath_missingRoot() {
        final Configuration config = new Configuration();

        try {
            ConfigUtil.getUsecaseRootPath(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }
}
