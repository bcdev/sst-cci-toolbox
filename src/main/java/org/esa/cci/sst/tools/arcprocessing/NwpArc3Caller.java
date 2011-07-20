/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Creates an ARC3 call.
 *
 * @author Thomas Storm
 */
class NwpArc3Caller {

    private static final String SETUP = "if [ ! -d \"$MMS_HOME\" ]\n" +
                                        "then\n" +
                                        "    PRGDIR=`dirname $0`\n" +
                                        "    export MMS_HOME=`cd \"$PRGDIR/..\" ; pwd`\n" +
                                        "fi\n\n" +
                                        "set -e # one fails, all fail\n\n" +
                                        '\n' +
                                        "if [ -z \"$MMS_HOME\" ]; then\n" +
                                        "    echo\n" +
                                        "    echo Error:\n" +
                                        "    echo MMS_HOME does not exists in your environment. Please\n" +
                                        "    echo set the MMS_HOME variable in your environment to the\n" +
                                        "    echo location of your CCI SST installation.\n" +
                                        "    echo\n" +
                                        "    exit 2\n" +
                                        "fi\n\n" +
                                        "MMS_OPTIONS=\"\"\n" +
                                        "if [ ! -z $MMS_DEBUG ]; then\n" +
                                        "    MMS_OPTIONS=\"-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y\"\n" +
                                        "fi\n\n" +
                                        "export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/home/tstorm/opt/local/lib\n" +
                                        "export PATH=${PATH}:/home/tstorm/opt/local/bin\n\n";
    private final Properties configuration;

    NwpArc3Caller(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    String createNwpArc3Call() {
        final String sensorName = configuration.getProperty(Constants.PROPERTY_MMS_NWP_ARC3_SENSOR);
        final String configurationFilePath = String.format("config/mms-config_%s.properties", sensorName);
        final String sensorPattern = configuration.getProperty(Constants.PROPERTY_MMS_NWP_ARC3_INPUT_PATTERN);
        final String nwpSourceDir = configuration.getProperty(Constants.PROPERTY_MMS_NWP_SOURCEDIR);
        final String nwpOutput = getUniqueOutputName(Constants.PROPERTY_MMS_NWP_TARGETFILE);
        final String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        final String arc3ConfigurationFile = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_CONFIG_FILE, "MMD_AATSR.INP");
        final String arc3Output = getUniqueOutputName(Constants.PROPERTY_MMS_ARC3_OUTPUT);
        final String nwpSourceFile = configuration.getProperty(Constants.PROPERTY_MMS_NWP_SOURCEFILE);

        final StringBuilder nwpArc3Call = new StringBuilder();

        nwpArc3Call.append(SETUP);
        final String mmdCall = String.format("bin/mmsmmd.sh -c %s -Dmms.target.filename=%s\n\n", configurationFilePath, nwpSourceFile);
        final String nwpCall = String.format(
                "java \\\n" +
                "    -Dmms.home=\"$MMS_HOME\" \\\n" +
                "    -javaagent:\"$MMS_HOME/lib/openjpa-all-2.1.0.jar\" \\\n" +
                "    -Xmx1024M $MMS_OPTIONS \\\n" +
                "    -classpath \"$MMS_HOME/lib/*\" \\\n" +
                "    org.esa.cci.sst.tools.nwp.Nwp \"%s\" \"%s\" \"false\" \"%s\" \"%s\" \"%s\"\n\n", sensorName, sensorPattern, nwpSourceFile, nwpSourceDir, nwpOutput);
        final String copyNwpOutputToEddie = String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n\n", nwpOutput, arc3home);
        final String callArc3 = String.format("ssh eddie.ecdf.ed.ac.uk \"cd %s ; ./MMD_SCREEN_Linux %s %s %s %s\"",
                                              arc3home, arc3ConfigurationFile, nwpOutput, nwpOutput, arc3Output);

        nwpArc3Call.append(mmdCall);
        nwpArc3Call.append(nwpCall);
        nwpArc3Call.append(copyNwpOutputToEddie);
        nwpArc3Call.append(callArc3);
        return nwpArc3Call.toString();
    }

    String createReingestionCall() {
        final String sensorName = configuration.getProperty(Constants.PROPERTY_MMS_NWP_ARC3_SENSOR);
        final String configurationFilePath = String.format("config/mms-config_%s.properties", sensorName);
        final String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        final String arc3Output = getUniqueOutputName(Constants.PROPERTY_MMS_ARC3_OUTPUT);
        final String arc3Pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "0");
        final String nwpOutput = getUniqueOutputName(Constants.PROPERTY_MMS_NWP_TARGETFILE);
        final String nwpPattern = configuration.getProperty(Constants.PROPERTY_MMS_NWP_PATTERN, "0");

        final StringBuilder builder = new StringBuilder();
        builder.append("if [ -z \"$CCI_SST_HOME\" ]; then \n");
        builder.append("    echo \n");
        builder.append("    echo Error:\n");
        builder.append("    echo CCI_SST_HOME does not exists in your environment. Please\n");
        builder.append("    echo set the CCI_SST_HOME variable in your environment to the\n");
        builder.append("    echo location of your CCI SST installation.\n");
        builder.append("    echo\n");
        builder.append("    exit 2\n");
        builder.append("fi\n\n");
        builder.append(String.format("scp eddie.ecdf.ed.ac.uk:%s/%s .\n\n", arc3home, arc3Output));
        builder.append(String.format("$CCI_SST_HOME/bin/mmsreingestmmd.sh \\\n" +
                                     " -Dmms.reingestion.filename=%s \\\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=arc3 \\\n" +
                                     " -Dmms.reingestion.pattern=%s \\\n" +
                                     " -c $CCI_SST_HOME/%s\n\n", arc3Output, arc3Pattern, configurationFilePath));

        builder.append(String.format("$CCI_SST_HOME/bin/mmsreingestmmd.sh \\\n" +
                                     " -Dmms.reingestion.filename=%s \\\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=nwp \\\n" +
                                     " -Dmms.reingestion.pattern=%s \\\n" +
                                     " -c $CCI_SST_HOME/%s", nwpOutput, nwpPattern, configurationFilePath));
        return builder.toString();
    }

    String createCleanupCall(String... scripts) {
        final StringBuilder builder = new StringBuilder();
        for (String script : scripts) {
            builder.append(String.format("rm %s\n", script));
        }
        final String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        final String arc3Output = getUniqueOutputName(Constants.PROPERTY_MMS_ARC3_OUTPUT);
        builder.append(String.format("\nssh eddie.ecdf.ed.ac.uk \"cd %s ; rm %s\"", arc3home, arc3Output));
        return builder.toString();
    }

    String getUniqueOutputName(String sourceOutputName) {
        String outputName = configuration.getProperty(sourceOutputName);
        final StringBuilder builder = new StringBuilder(outputName);
        final Date date = new Date();
        final String time = new SimpleDateFormat("yyyyMMddHHmm").format(date);
        builder.insert(outputName.lastIndexOf('.'), '_' + time);
        return builder.toString();
    }
}
