package org.esa.cci.sst.util;


import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

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

    @Test
    public void testGetTimeRange() throws ParseException {
        final Configuration config = new Configuration();
        config.put("start", "1978-01-01T00:00:00Z");
        config.put("stop", "1979-01-01T00:00:00Z");

        final TimeRange timeRange = ConfigUtil.getTimeRange("start", "stop", config);
        assertNotNull(timeRange);
        final Date startDate = timeRange.getStartDate();
        assertEquals(TimeUtil.parseCcsdsUtcFormat("1978-01-01T00:00:00Z").getTime(), startDate.getTime());

        final Date stopDate = timeRange.getStopDate();
        assertEquals(TimeUtil.parseCcsdsUtcFormat("1979-01-01T00:00:00Z").getTime(), stopDate.getTime());
    }

    @Test
    public void testGetTimeRange_throwsOnMissingStartValue() throws ParseException {
        final Configuration config = new Configuration();
        config.put("stop", "1979-01-01T00:00:00Z");

        try {
            ConfigUtil.getTimeRange("start", "stop", config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testGetTimeRange_throwsOnMissingStopValue() throws ParseException {
        final Configuration config = new Configuration();
        config.put("start", "1978-01-01T00:00:00Z");

        try {
            ConfigUtil.getTimeRange("start", "stop", config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testGetTimeRange_throwsWhenStopIsBeforeStart() throws ParseException {
        final Configuration config = new Configuration();
        config.put("start", "1980-01-01T00:00:00Z");
        config.put("stop", "1979-01-01T00:00:00Z");

        try {
            ConfigUtil.getTimeRange("start", "stop", config);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Configuration error: stop is before start", expected.getMessage());
        }
    }
}
