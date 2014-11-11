package org.esa.cci.sst.util;


import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tool.ToolException;

import java.io.File;

public class FileUtil {

    public static File getExistingFile(String path, String toolHome) {
        final File file = getFile(path, toolHome);
        if (file != null) {
            if (!file.isFile()) {
                final String message = String.format("File '%s': is not existing", file);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
        }
        return file;
    }

    public static File getExistingDirectory(String directory, String toolHome) {
        final File dir = getFile(directory, toolHome);
        if (dir != null) {
            if (!dir.isDirectory()) {
                final String message = String.format("Directory '%s': is not existing", dir);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }

            if (!dir.canWrite()) {
                final String message = String.format("Directory '%s': is not writable", dir);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
        }
        return dir;
    }

    // package access for testing only tb 2014-11-11
    static File getFile(String path, String toolHome) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        if (!(path.startsWith(".") || new File(path).isAbsolute())) {
            return new File(toolHome, path);
        }

        return new File(path);
    }
}
