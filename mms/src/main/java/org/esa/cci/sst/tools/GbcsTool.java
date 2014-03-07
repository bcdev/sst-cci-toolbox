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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

public class GbcsTool extends BasicTool {

    private static final String KEY_MMS_GBCS_INTELVERSION = "mms.gbcs.intelversion";
    private static final String KEY_MMS_GBCS_VERSION = "mms.gbcs.version";
    private static final String KEY_MMS_GBCS_HOME = "mms.gbcs.home";
    private static final String KEY_MMS_GBCS_MMD_SOURCE = "mms.gbcs.mmd.source";
    private static final String KEY_MMS_GBCS_NWP_SOURCE = "mms.gbcs.nwp.source";
    private static final String KEY_MMS_GBCS_MMD_TARGET = "mms.gbcs.mmd.target";
    private static final String KEY_MMS_GBCS_SENSOR = "mms.gbcs.sensor";

    private static final String TEMPLATE =
            String.format(
                    "#! /bin/sh\nmodule load intel/${%s}\n${%s}/${%s}/bin/MMD_SCREEN_Linux ${%s}/${%s}/dat_cci/${INP} ${%s} ${%s} ${%s}",
                    KEY_MMS_GBCS_INTELVERSION, KEY_MMS_GBCS_HOME, KEY_MMS_GBCS_VERSION, KEY_MMS_GBCS_HOME,
                    KEY_MMS_GBCS_VERSION, KEY_MMS_GBCS_MMD_SOURCE, KEY_MMS_GBCS_NWP_SOURCE, KEY_MMS_GBCS_MMD_TARGET);

    private String sensorName;
    private Properties properties;

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

        properties = new Properties();
        properties.put(KEY_MMS_GBCS_INTELVERSION, config.getStringValue(KEY_MMS_GBCS_INTELVERSION));
        properties.put(KEY_MMS_GBCS_VERSION, config.getStringValue(KEY_MMS_GBCS_VERSION));
        properties.put(KEY_MMS_GBCS_HOME, config.getStringValue(KEY_MMS_GBCS_HOME));
        properties.put(KEY_MMS_GBCS_MMD_SOURCE, config.getStringValue(KEY_MMS_GBCS_MMD_SOURCE));
        properties.put(KEY_MMS_GBCS_NWP_SOURCE, config.getStringValue(KEY_MMS_GBCS_NWP_SOURCE));
        properties.put(KEY_MMS_GBCS_MMD_TARGET, config.getStringValue(KEY_MMS_GBCS_MMD_TARGET));

        sensorName = config.getStringValue(KEY_MMS_GBCS_SENSOR);
    }

    private void run() throws IOException, InterruptedException {
        final String inputFilename = getInputFilename(sensorName);
        properties.put("INP", inputFilename);

        final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
        final String resolvedTemplate = ProcessRunner.resolveTemplate(TEMPLATE, properties);

        runner.execute(ProcessRunner.writeExecutableScript(resolvedTemplate, "gbcs", ".sh").getPath());
    }

    static String getInputFilename(String sensorName) {
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
                        MessageFormat.format("Illegal value ''{0}'' for key ''{1}''.", sensorName, KEY_MMS_GBCS_SENSOR),
                        ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

}
