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

import org.esa.cci.sst.util.TimeUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

/**
 * The configuration used for the {@link org.esa.cci.sst.regavg.AveragingTool}.
 *
 * @author Norman Fomferra
 */
public class OldConfiguration {

    private final Properties properties;

    public OldConfiguration(Properties properties) {
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

    public Date getDate(Parameter parameter, boolean mandatory) throws ToolException {
        String string = getString(parameter, mandatory);
        if (string == null) {
            return null;
        }
        try {
            return TimeUtil.parseShortUtcFormat(string);
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
