package org.esa.cci.sst.tools.mmdgeneration;


import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DimensionConfigurationInitializer {

    public static Map<String, Integer> initialize(Collection<String> dimensionNames, Configuration configuration) {
        final Properties dimensionProperties = readDimensionProperties(configuration);

        return parseDimensionConfiguration(dimensionNames, dimensionProperties);
    }

    // package access for testing only tb 2014-03-10
    static Properties readDimensionProperties(Configuration configuration) {
        final String configFilePath = configuration.getStringValue(Configuration.KEY_MMS_MMD_DIMENSIONS);

        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configFilePath));
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (IOException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_IO_ERROR);
        }
        return properties;
    }

    // package access for testing only tb 2014-03-10
    static Map<String, Integer> parseDimensionConfiguration(Collection<String> dimensionNames, Properties properties) {
        final Map<String, Integer> dimensionConfiguration = new HashMap<>(50);
        for (final String dimensionName : dimensionNames) {
            if (Constants.DIMENSION_NAME_MATCHUP.equals(dimensionName)) {
                continue;
            }
            final String dimensionLength = properties.getProperty(dimensionName);
            if (StringUtils.isEmpty(dimensionLength)) {
                throw new ToolException(
                        MessageFormat.format("Length of dimension ''{0}'' is not configured.", dimensionName),
                        ToolException.TOOL_CONFIGURATION_ERROR);
            }
            try {
                dimensionConfiguration.put(dimensionName, Integer.parseInt(dimensionLength));
            } catch (NumberFormatException e) {
                throw new ToolException(
                        MessageFormat.format("Cannot parse length of dimension ''{0}''.", dimensionName),
                        ToolException.TOOL_CONFIGURATION_ERROR);
            }
        }

        return dimensionConfiguration;
    }
}
