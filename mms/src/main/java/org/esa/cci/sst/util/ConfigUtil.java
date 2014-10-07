package org.esa.cci.sst.util;


import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.samplepoint.TimeRange;

import java.io.File;
import java.util.Date;

public class ConfigUtil {

    public static String getUsecaseRootPath(Configuration config) {
        final String archiveRootPath = config.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT);

        final String usecase;
        try {
            usecase = config.getStringValue(Configuration.KEY_MMS_USECASE);
        } catch (Exception e) {
            return archiveRootPath;
        }

        return archiveRootPath + File.separatorChar + usecase;
    }

    public static TimeRange getTimeRange(String startKey, String stopKey, Configuration config) {
        final Date startDate = config.getDateValue(startKey);
        final Date stopDate = config.getDateValue(stopKey);
        if (!stopDate.after(startDate)) {
            throw new ToolException("Configuration error: " + stopKey + " is before " + startKey, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return new TimeRange(startDate, stopDate);
    }
}
