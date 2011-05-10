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
        super(configuration);
        this.configuration = new Properties(configuration);
    }

    @Override
    public String createArc3Call() {
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        String executableName = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_EXECUTABLE, "MMD_SCREEN_Linux");
        String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                          getDefaultTargetFileName(sourceFilename));
        String nwpFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "test_nwp.nc");

        final StringBuilder arc3Call = new StringBuilder();
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:tmp/\n", sourceFilename));
        arc3Call.append(String.format("ssh eddie.ecdf.ed.ac.uk ./%s MDB.INP %s %s %s", executableName, sourceFilename,
                                      nwpFilename, targetFilename));
        return arc3Call.toString();
    }

    @Override
    public String createReingestionCall() {
        final String sourceFilename = getSourceFilename();
        final String targetFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_TARGETFILE,
                                                                getDefaultTargetFileName(sourceFilename));
        final String pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "20000");

        final StringBuilder builder = new StringBuilder();
        builder.append("ssh eddie.ecdf.ed.ac.uk ");
        builder.append(String.format("bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                     " -Dmms.reingestion.pattern=%s \\\n" +
                                     " -c config/mms-config-eddie1.properties", targetFilename, pattern));
        return builder.toString();
    }

    @Override
    public String createCleanupCall() {
        final String sourceFilename = getSourceFilename();
        return String.format("ssh eddie.ecdf.ed.ac.uk rm %s", sourceFilename);
    }
}
