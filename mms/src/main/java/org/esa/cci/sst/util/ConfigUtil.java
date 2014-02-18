package org.esa.cci.sst.util;


import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;

import java.io.File;

public class ConfigUtil {

    public static String getArchiveRootPath(Configuration config) {
        final String archiveRootPath = config.getStringValue(Configuration.KEY_ARCHIVE_ROOTDIR);
        if (StringUtils.isEmpty(archiveRootPath)) {
            throw new ToolException("Missing archive root in config.", -1);
        }

        final String useCase = config.getStringValue(Configuration.KEY_ARCHIVE_USECASE);
        if (StringUtils.isEmpty(useCase)) {
            return archiveRootPath;
        }

        return archiveRootPath + File.separatorChar + useCase;
    }
}
