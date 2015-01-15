/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.util;


import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tool.ToolException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

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

    public static File createNewFile(String filePath) throws IOException {
        final File file = new File(filePath);
        final File parentDir = file.getParentFile();
        if (!parentDir.isDirectory()) {
            if (!parentDir.mkdirs()) {
                final String message = MessageFormat.format("Unable to create directory: {0}",
                                                            parentDir.getAbsolutePath());
                throw new ToolException(message, ToolException.TOOL_ERROR);
            }
        }
        if (file.exists()) {
            file.delete();
        }
        if (!file.createNewFile()) {
            final String message = String.format("Unable to create file '%s'", filePath);
            throw new ToolException(message, ToolException.TOOL_IO_ERROR);
        }
        return file;
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
