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
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Creates an ARC3 call.
 *
 * @author Thomas Storm
 */
class Arc3CallBuilder {

    private final Properties configuration;

    Arc3CallBuilder(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    protected String createArc3Call() {
        String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                          getDefaultTargetFileName(sourceFilename));

        final StringBuilder arc3Call = new StringBuilder();
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n", sourceFilename, arc3home));
        arc3Call.append(String.format("ssh eddie.ecdf.ed.ac.uk \"cd %s ; ./MMD_SCREEN_Linux MDB.INP %s %s %s \"\n",
                                      arc3home, sourceFilename, sourceFilename, targetFilename));
        arc3Call.append(String.format("scp eddie.ecdf.ed.ac.uk:%s/%s .\n", arc3home, targetFilename));
        return arc3Call.toString();
    }

    protected String createReingestionCall() {
        final String sourceFilename = getSourceFilename();
        final String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                                getDefaultTargetFileName(sourceFilename));
        final String pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "0");

        final StringBuilder builder = new StringBuilder();
        builder.append("if [ -z \"$CCI_SST_HOME\" ]; then \n");
        builder.append("    echo \n");
        builder.append("    echo Error:\n");
        builder.append("    echo CCI_SST_HOME does not exists in your environment. Please\n");
        builder.append("    echo set the CCI_SST_HOME variable in your environment to the\n");
        builder.append("    echo location of your CCI SST installation.\n");
        builder.append("    echo\n");
        builder.append("    exit 2\n");
        builder.append("fi\n");
        builder.append(String.format("$CCI_SST_HOME/bin/mmsreingestmmd.sh \\\n" +
                                     " -Dmms.reingestion.filename=%s \\\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                     " -Dmms.reingestion.pattern=%s \\\n" +
                                     " -c $CCI_SST_HOME/config/mms-config.properties", targetFilename, pattern));
        return builder.toString();
    }

    private String getSourceFilename() {
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        return sourceFilename;
    }

    String createCleanupCall(String... scripts) {
        final StringBuilder builder = new StringBuilder();
        for(String script: scripts) {
            builder.append(String.format("rm %s\n", script));
        }
        return builder.toString();
    }

    static String getDefaultTargetFileName(String sourceFilename) {
        final int extensionIndex = sourceFilename.lastIndexOf(".nc");
        return String.format("%s_ARC3.nc", sourceFilename.substring(0, extensionIndex));
    }

    static void validateSourceFilename(String sourceFilename) {
        try {
            if (sourceFilename == null || !NetcdfFile.canOpen(sourceFilename)) {
                throw new IOException();
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    MessageFormat.format("Property ''{0}'' must be set, and file must be readable.",
                                         Constants.PROPERTY_MMS_ARC3_SOURCEFILE));
        }
    }
}
