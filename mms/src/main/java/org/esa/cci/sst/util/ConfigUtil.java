package org.esa.cci.sst.util;


import org.esa.cci.sst.tools.Configuration;

import java.io.File;

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
}
