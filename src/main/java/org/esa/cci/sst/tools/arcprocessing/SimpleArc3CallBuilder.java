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

import java.util.Properties;

/**
 * @author Thomas Storm
 */
class SimpleArc3CallBuilder extends Arc3CallBuilder {

    private final Properties configuration;

    SimpleArc3CallBuilder(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    @Override
    protected String createArc3Call() {
        String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                          getDefaultTargetFileName(sourceFilename));
        String nwpFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "test_nwp.nc");

        final StringBuilder arc3Call = new StringBuilder();
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n", sourceFilename, arc3home));
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n", nwpFilename, arc3home));
        arc3Call.append(String.format("ssh eddie.ecdf.ed.ac.uk \"cd %s ; ./MMD_SCREEN_Linux MDB.INP %s %s %s \"\n",
                                      arc3home, sourceFilename, nwpFilename, targetFilename));
        arc3Call.append(String.format("scp eddie.ecdf.ed.ac.uk:%s/%s .\n", arc3home, targetFilename));
        return arc3Call.toString();
    }

    @Override
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
}
