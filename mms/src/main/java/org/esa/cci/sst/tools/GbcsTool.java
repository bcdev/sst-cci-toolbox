package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.util.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

public class GbcsTool extends BasicTool {

    private static final String KEY_CONFIG_FILENAME = "CONFIG";
    private static final String TEMPLATE =
            String.format(
                    "modulecmd sh load intel/${%s} && " +
                    "${%s}/${%s}/bin/MMD_SCREEN_Linux ${%s}/${%s}/dat_cci/${%s} ${%s} ${%s} ${%s}",
                    Configuration.KEY_MMS_GBCS_INTELVERSION,
                    Configuration.KEY_MMS_GBCS_HOME,
                    Configuration.KEY_MMS_GBCS_VERSION,
                    Configuration.KEY_MMS_GBCS_HOME,
                    Configuration.KEY_MMS_GBCS_VERSION,
                    KEY_CONFIG_FILENAME,
                    Configuration.KEY_MMS_GBCS_MMD_SOURCE,
                    Configuration.KEY_MMS_GBCS_NWP_SOURCE,
                    Configuration.KEY_MMS_GBCS_MMD_TARGET);

    private String sensorName;
    private Properties properties;
    private String mmdSource;
    private String nwpSource;
    private String mmdTarget;

    public GbcsTool() {
        super("gbcs-tool", "1.0");
    }

    public static void main(String[] args) {
        final GbcsTool tool = new GbcsTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        // no database functions needed, therefore don't call
        // super.initialize();
        final Configuration config = getConfig();

        mmdSource = config.getStringValue(Configuration.KEY_MMS_GBCS_MMD_SOURCE);
        nwpSource = config.getStringValue(Configuration.KEY_MMS_GBCS_NWP_SOURCE);
        mmdTarget = config.getStringValue(Configuration.KEY_MMS_GBCS_MMD_TARGET);
        sensorName = config.getStringValue(Configuration.KEY_MMS_GBCS_SENSOR);

        properties = new Properties();
        properties.put(Configuration.KEY_MMS_GBCS_INTELVERSION,
                       config.getStringValue(Configuration.KEY_MMS_GBCS_INTELVERSION));
        properties.put(Configuration.KEY_MMS_GBCS_VERSION,
                       config.getStringValue(Configuration.KEY_MMS_GBCS_VERSION));
        properties.put(Configuration.KEY_MMS_GBCS_HOME,
                       config.getStringValue(Configuration.KEY_MMS_GBCS_HOME));
        properties.put(Configuration.KEY_MMS_GBCS_MMD_SOURCE, mmdSource);
        properties.put(Configuration.KEY_MMS_GBCS_NWP_SOURCE, nwpSource);
        properties.put(Configuration.KEY_MMS_GBCS_MMD_TARGET, mmdTarget);
    }

    private void run() throws IOException, InterruptedException {
        final String configFilename = getConfigurationFilename(sensorName);
        properties.put(KEY_CONFIG_FILENAME, configFilename);

        if (doesNotExist(mmdSource)) {
            return;
        }
        if (doesNotExist(nwpSource)) {
            return;
        }

        final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
        final String resolvedTemplate = ProcessRunner.resolveTemplate(TEMPLATE, properties);

        runner.execute(resolvedTemplate);
    }

    private boolean doesNotExist(String path) {
        final boolean exists = new File(path).exists();
        if (!exists) {
            getLogger().warning(MessageFormat.format("missing source file: {0}", path));
            getLogger().warning(MessageFormat.format("skipping target file: {0}", mmdTarget));
            return true;
        }
        return false;
    }

    // package public for testing
    static String getConfigurationFilename(String sensorName) {
        switch (sensorName) {
            case "atsr.1":
                return "MMD_ATSR1.inp";
            case "atsr.2":
                return "MMD_ATSR2.inp";
            case "atsr.3":
                return "MMD_AATSR.inp";
            case "avhrr.n10":
                return "MMD_NOAA10.inp";
            case "avhrr.n11":
                return "MMD_NOAA11.inp";
            case "avhrr.n12":
                return "MMD_NOAA12.inp";
            case "avhrr.n14":
                return "MMD_NOAA14.inp";
            case "avhrr.n15":
                return "MMD_NOAA15.inp";
            case "avhrr.n16":
                return "MMD_NOAA16.inp";
            case "avhrr.n17":
                return "MMD_NOAA17.inp";
            case "avhrr.n18":
                return "MMD_NOAA18.inp";
            case "avhrr.n19":
                return "MMD_NOAA19.inp";
            case "avhrr.m02":
                return "MMD_METOP02.inp";
            default:
                throw new ToolException(
                        MessageFormat.format("Illegal value for key ''{0}'': ''{1}''.",
                                             Configuration.KEY_MMS_GBCS_SENSOR,
                                             sensorName),
                        ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

}
