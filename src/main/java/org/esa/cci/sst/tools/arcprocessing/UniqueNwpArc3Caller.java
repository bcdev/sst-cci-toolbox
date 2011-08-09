/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.text.ParseException;
import java.util.Properties;

/**
 * Creates an ARC3 call which does everything in the submit call and produces an empty reingestion call.
 *
 * @author Thomas Storm
 */
public class UniqueNwpArc3Caller implements NwpArc3Caller {

    private final Properties configuration;

    UniqueNwpArc3Caller(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    @Override
    public String createNwpArc3Call() throws ParseException {
        final String sensorName = configuration.getProperty(Constants.PROPERTY_MMS_NWP_ARC3_SENSOR);
        final String startTime = configuration.getProperty(Constants.PROPERTY_NWP_ARC3_START_TIME);
        final String stopTime = configuration.getProperty(Constants.PROPERTY_NWP_ARC3_STOP_TIME);
        final String archiveRootPath = configuration.getProperty(Constants.PROPERTY_ARCHIVE_ROOTDIR);
        String configurationFilePath = configuration.getProperty(Constants.PROPERTY_CONFIGURATION);
        final String sensorPattern = configuration.getProperty(Constants.PROPERTY_MMS_NWP_ARC3_INPUT_PATTERN);
        final String nwpSourceDir = configuration.getProperty(Constants.PROPERTY_MMS_NWP_SOURCEDIR);
        final String nwpSourceFile = createOutputFilename(sensorName, "sub", startTime, stopTime);
        final String nwpOutput = createOutputFilename(sensorName, "nwp", startTime, stopTime);
        final String arc3Output = createOutputFilename(sensorName, "arc3", startTime, stopTime);
        final String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        final String arc3ConfigurationFile = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_CONFIG_FILE, "MMD_AATSR.inp");
        String nwpDestDir = configuration.getProperty(Constants.PROPERTY_NWP_DESTDIR, ".");
        String arc3DestDir = configuration.getProperty(Constants.PROPERTY_ARC3_DESTDIR, ".");
        final String arc3Pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "0");
        final String nwpPattern = configuration.getProperty(Constants.PROPERTY_MMS_NWP_PATTERN, "0");
        final String mmdVariablesPath = String.format("config/mmd-variables_%s.config", sensorName);

        if (! arc3DestDir.startsWith(File.separator)) {
            arc3DestDir = archiveRootPath + File.separator + arc3DestDir;
        }
        if (! nwpDestDir.startsWith(File.separator)) {
            nwpDestDir = archiveRootPath + File.separator + nwpDestDir;
        }
        configurationFilePath = new File(configurationFilePath).getAbsolutePath();
//        if (! new File(mmdVariablesPath).exists()) {
//            throw new ToolException(String.format("missing configuration %s", mmdVariablesPath), ToolException.TOOL_CONFIGURATION_ERROR);
//        }

        final StringBuilder nwpArc3Call = new StringBuilder();

        nwpArc3Call.append(SETUP);
        final String mmdCall = String.format("bin/mmsmmd.sh -c %s -Dmms.target.filename=%s -Dmms.target.variables=$MMS_HOME/%s " +
                                                     "-Dmms.target.startTime=%s -Dmms.target.stopTime=%s\n\n",
                                             configurationFilePath, nwpSourceFile, mmdVariablesPath, startTime, stopTime);
        final String nwpCall = String.format(
                "java \\\n" +
                "    -Dmms.home=\"$MMS_HOME\" \\\n" +
                "    -javaagent:\"$MMS_HOME/lib/openjpa-all-2.1.0.jar\" \\\n" +
                "    -Xmx1024M $MMS_OPTIONS \\\n" +
                "    -classpath \"$MMS_HOME/lib/*\" \\\n" +
                "    org.esa.cci.sst.tools.nwp.Nwp \"%s\" \"%s\" \"false\" \"%s\" \"%s\" \"%s\"\n\n", sensorName, sensorPattern, nwpSourceFile, nwpSourceDir, nwpOutput);
        final String copyNwpOutputToArc3Home = String.format("cp %s %s\n\n", nwpOutput, arc3home);
        final String goToArc3Home = String.format("cd %s\n\n", arc3home);
        final String callArc3 = String.format("./MMD_SCREEN_Linux %s %s %s %s\n\n", arc3ConfigurationFile, nwpOutput, nwpOutput, arc3Output);

        final String copyArc3OutputToDestDir = String.format("cp %s/%s %s/%s\n\n", arc3home, arc3Output, arc3DestDir, arc3Output);
        final String reingestArc3Output = String.format("$MMS_HOME/bin/mmsreingestmmd.sh \\\n" +
                                                        " -Dmms.reingestion.filename=%s/%s \\\n" +
                                                        " -Dmms.reingestion.located=no \\\n" +
                                                        " -Dmms.reingestion.sensor=arc3 \\\n" +
                                                        " -Dmms.reingestion.pattern=%s \\\n" +
                                                        " -c %s\n\n", arc3DestDir, arc3Output, arc3Pattern, configurationFilePath);

        final String copyNwpOutputToDestDir = String.format("cp %s %s/%s\n\n", nwpOutput, nwpDestDir, nwpOutput);
        final String reingestNwpOutput = String.format("$MMS_HOME/bin/mmsreingestmmd.sh \\\n" +
                                                       " -Dmms.reingestion.filename=%s/%s \\\n" +
                                                       " -Dmms.reingestion.located=no \\\n" +
                                                       " -Dmms.reingestion.sensor=nwp \\\n" +
                                                       " -Dmms.reingestion.pattern=%s \\\n" +
                                                       " -c %s", nwpDestDir, nwpOutput, nwpPattern, configurationFilePath);


        nwpArc3Call.append(mmdCall);
        nwpArc3Call.append(nwpCall);
        nwpArc3Call.append(copyNwpOutputToArc3Home);
        nwpArc3Call.append(goToArc3Home);
        nwpArc3Call.append(callArc3);
        nwpArc3Call.append(copyArc3OutputToDestDir);
        nwpArc3Call.append(reingestArc3Output);
        nwpArc3Call.append(copyNwpOutputToDestDir);
        nwpArc3Call.append(reingestNwpOutput);
        return nwpArc3Call.toString();
    }

    @Override
    public String createReingestionCall() {
        return "";
    }

    @Override
    public String createCleanupCall(String... scripts) {
        final StringBuilder builder = new StringBuilder();
        for (String script : scripts) {
            builder.append(String.format("rm %s\n", script));
        }
        return builder.toString();
    }

    String createOutputFilename(String sensor, String type, String startTime, String stopTime) throws ParseException {
        String start = TimeUtil.formatCompactUtcFormat(TimeUtil.parseCcsdsUtcFormat(startTime));
        String stop  = TimeUtil.formatCompactUtcFormat(TimeUtil.parseCcsdsUtcFormat(stopTime));
        // atsr.3-nwp-20100602015655-20100602020155.nc, types nwp, arc3, sub
        return String.format("%s-%s-%s-%s.nc", sensor, type, start, stop);
    }

}