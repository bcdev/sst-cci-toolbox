package org.esa.cci.sst.tool;

import org.esa.cci.sst.util.UTC;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

/**
 * The configuration used for the {@link org.esa.cci.sst.regavg.RegionalAverageTool}.
 *
 * @author Norman Fomferra
 */
public class Configuration {

    private Properties properties;

    public Configuration(Properties properties) {
        this.properties = properties;
    }

    public String getString(Parameter parameter, boolean mandatory) throws ToolException {
        return getString(parameter.getName(), parameter.getDefaultValue(), mandatory);
    }

    public String getString(String name, String defaultValue, boolean mandatory) throws ToolException {
        String property = properties.getProperty(name, defaultValue);
        if (property == null && mandatory) {
            throw new ToolException("Missing value for mandatory parameter '" + name + "'", ExitCode.USAGE_ERROR);
        }
        return property;
    }

    public File getFile(Parameter parameter, boolean mandatory) throws ToolException {
        String pathname = getString(parameter, mandatory);
        if (pathname == null) {
            return null;
        }
        return new File(pathname);
    }

    public File getExistingFile(Parameter parameter, boolean mandatory) throws ToolException {
        File file = getFile(parameter, mandatory);
        if (file != null) {
            if (!file.exists()) {
               throw new ToolException(String.format("Parameter '%s': File not found: %s", parameter.getName(), file), ExitCode.IO_ERROR);
            }
            if (!file.isFile()) {
                throw new ToolException(String.format("Parameter '%s': Value is not a file: '%s'", parameter.getName(), file), ExitCode.IO_ERROR);
            }
        }
        return file;
    }

    public File getExistingDirectory(Parameter parameter, boolean mandatory) throws ToolException {
        File dir = getFile(parameter, mandatory);
        if (dir != null) {
            if (!dir.exists()) {
               throw new ToolException(String.format("Parameter '%s': Directory not found: '%s'", parameter.getName(), dir), ExitCode.IO_ERROR);
            }
            if (!dir.isDirectory()) {
                throw new ToolException(String.format("Parameter '%s': Value is not a directory: '%s'", parameter.getName(), dir), ExitCode.IO_ERROR);
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
            return UTC.getDateFormat("yyyyMMdd").parse(string);
        } catch (ParseException e) {
            throw new ToolException(String.format("Parameter '%s': Value is not a valid date: '%s'", parameter.getName(), string), ExitCode.USAGE_ERROR);
        }
    }


}
