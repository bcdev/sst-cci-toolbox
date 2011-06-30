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

import java.io.IOException;
import java.util.Properties;

/**
 * the strategy to create subscenes and ARC3 them:
 * - obtain the source file
 * - cut out subscenes using database information
 * - put these into a new mmd file
 * - copy all old values into that file
 * - take that file as input for arc 3
 * - reingest the new file
 * - delete the scripts
 *
 * @author Thomas Storm
 */
class SubsceneArc3CallBuilder extends Arc3CallBuilder {

    private final Properties configuration;

    SubsceneArc3CallBuilder(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    @Override
    public String createArc3Call() throws IOException {
        final String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        String arc3home = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_HOME);
        final String targetFilename = createSubsceneMmdFilename(sourceFilename);

        String nwpFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_NWPFILE, "test_nwp.nc");

        final StringBuilder arc3Call = new StringBuilder();
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n", sourceFilename, arc3home));
        arc3Call.append(String.format("scp %s eddie.ecdf.ed.ac.uk:%s\n", nwpFilename, arc3home));
        arc3Call.append(String.format("ssh eddie.ecdf.ed.ac.uk \" cd %s ; ./MMD_SCREEN_Linux MDB.INP %s %s %s",
                                      arc3home, targetFilename, nwpFilename, getDefaultTargetFileName(targetFilename)));

        return arc3Call.toString();
    }

    @Override
    protected String createReingestionCall() {
        final String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        final String subsceneMmdFilename = createSubsceneMmdFilename(sourceFilename);
        final String pattern = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_PATTERN, "20000");

        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("scp eddie.ecdf.ed.ac.uk:%s . \n", subsceneMmdFilename));
        builder.append("if [ -z \"$CCI_SST_HOME\" ]; then ");
        builder.append("    echo ");
        builder.append("    echo Error:\n");
        builder.append("    echo CCI_SST_HOME does not exists in your environment. Please\n");
        builder.append("    echo set the CCI_SST_HOME variable in your environment to the\n");
        builder.append("    echo location of your CCI SST installation.\n");
        builder.append("    echo\n");
        builder.append("    exit 2\n");
        builder.append("fi\n");
        builder.append(String.format("$CCI_SST_HOME/bin/mmsreingestmmd.sh -Dmms.reingestion.filename=%s \\\n" +
                                     " -Dmms.reingestion.located=no \\\n" +
                                     " -Dmms.reingestion.sensor=ARC3 \\\n" +
                                     " -Dmms.reingestion.pattern=%s \\\n" +
                                     " -c $CCI_SST_HOME/config/mms-config.properties", subsceneMmdFilename, pattern));
        return builder.toString();


    }

    static String createSubsceneMmdFilename(String sourceFilename) {
        final int extensionStart = sourceFilename.lastIndexOf('.');
        final StringBuilder builder = new StringBuilder(sourceFilename);
        return builder.insert(extensionStart, "_subscenes").toString();
    }

}
