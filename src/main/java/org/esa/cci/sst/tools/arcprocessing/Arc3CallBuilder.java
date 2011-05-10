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
 * The interface the different strategies to create an ARC3 call need to implement.
 *
 * @author Thomas Storm
 */
abstract class Arc3CallBuilder {

    private final Properties configuration;

    protected Arc3CallBuilder(Properties configuration) {
        this.configuration = new Properties(configuration);
    }

    abstract String createArc3Call() throws IOException;

    abstract String createReingestionCall();

    abstract String createCleanupCall();

    String getSourceFilename() {
        String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        validateSourceFilename(sourceFilename);
        return sourceFilename;
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
