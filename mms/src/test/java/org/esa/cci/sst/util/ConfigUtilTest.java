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
    public void testGetArchiveRootPath() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, "/archive");
        config.put(Configuration.KEY_ARCHIVE_USECASE, "mms2");

        final String path = ConfigUtil.getArchiveRootPath(config);
        assertNotNull(path);
        assertEquals("/archive" + File.separatorChar + "mms2", path);
    }

    @Test
    public void testGetArchiveRootPath_missingUseCase() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_ARCHIVE_ROOTDIR, "/archive");

        final String path = ConfigUtil.getArchiveRootPath(config);
        assertNotNull(path);
        assertEquals("/archive", path);
    }

    @Test
    public void testGetArchiveRootPath_missingRoot() {
        final Configuration config = new Configuration();

        try {
            ConfigUtil.getArchiveRootPath(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }
}
