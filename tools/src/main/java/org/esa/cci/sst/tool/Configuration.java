/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.tool;

import org.esa.cci.sst.util.UTC;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

/**
 * The configuration used for the {@link org.esa.cci.sst.regavg.AveragingTool}.
 *
 * @author Norman Fomferra
 */
public class Configuration {

    private final String toolHome;
    private final Properties properties;

    public Configuration(String toolHome, Properties properties) {
        this.toolHome = toolHome;
        this.properties = properties;
    }

    public String getString(Parameter parameter, boolean mandatory) throws ToolException {
        return getString(parameter.getName(), parameter.getDefaultValue(), mandatory);
    }

    public String getString(String name, String defaultValue, boolean mandatory) throws ToolException {
        String property = properties.getProperty(name, defaultValue);
        if (property == null && mandatory) {
            throw new ToolException("Missing value for mandatory parameter '" + name + "'", ToolException.TOOL_USAGE_ERROR);
        }
        return property == null ? property : property.trim();
    }

    public File getFile(Parameter parameter, boolean mandatory) throws ToolException {
        String path = getString(parameter, mandatory);
        if (path == null) {
            return null;
        }
        if (!path.startsWith(".") && !new File(path).isAbsolute()) {
            return new File(toolHome, path);
        }
        return new File(path);
    }

    public File getExistingFile(Parameter parameter, boolean mandatory) throws ToolException {
        File file = getFile(parameter, mandatory);
        if (file != null) {
            if (!file.exists()) {
                final String message = String.format("Parameter '%s': File not found: %s", parameter.getName(), file);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
            if (!file.isFile()) {
                final String message = String.format("Parameter '%s': Value is not a file: '%s'", parameter.getName(), file);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
        }
        return file;
    }

    public File getExistingDirectory(Parameter parameter, boolean mandatory) throws ToolException {
        File dir = getFile(parameter, mandatory);
        if (dir != null) {
            if (!dir.exists()) {
                final String message = String.format("Parameter '%s': Directory not found: '%s'", parameter.getName(), dir);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
            if (!dir.isDirectory()) {
                final String message = String.format("Parameter '%s': Value is not a directory: '%s'", parameter.getName(), dir);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
            if (!dir.canWrite()) {
                final String message = String.format("Parameter '%s': Directory is not writable: '%s'", parameter.getName(), dir);
                throw new ToolException(message, ToolException.TOOL_IO_ERROR);
            }
        }
        return dir;
    }

    public Date getDate(Parameter parameter, boolean mandatory) throws ToolException {
        String string = getString(parameter, mandatory);
        if (string == null) {
            return null;
        }
        try {
            return UTC.getDateFormat("yyyy-MM-dd").parse(string);
        } catch (ParseException e) {
            final String message = String.format("Parameter '%s': Value is not a valid date: '%s'", parameter.getName(), string);
            throw new ToolException(message, ToolException.TOOL_USAGE_ERROR);
        }
    }


    public boolean getBoolean(Parameter parameter, boolean defaultValue) throws ToolException {
        String string = getString(parameter, false);
        if (string == null) {
            return defaultValue;
        }
        if (string.equalsIgnoreCase("true")) {
            return true;
        } else if (string.equalsIgnoreCase("false")) {
            return false;
        } else {
            final String message = String.format("Parameter '%s': Value is not a valid Boolean: '%s'", parameter.getName(), string);
            throw new ToolException(message, ToolException.TOOL_USAGE_ERROR);
        }
    }
}
