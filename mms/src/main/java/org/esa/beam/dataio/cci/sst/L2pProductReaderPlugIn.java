/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * @author Ralf Quast
 */
public class L2pProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "ESACCI-L2P-SST";
    private static final String FILE_EXTENSION_NC = ".nc";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File inputFile = new File(input.toString());
        final String inputFileName = inputFile.getName();
        if (!inputFileName.endsWith(FILE_EXTENSION_NC)) {
            return DecodeQualification.UNABLE;
        }
        if (matches(inputFileName)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    // package public for testing only
    static boolean matches(String filename) {
        return filename.matches("[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc");
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new L2pProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "SST-CCI L2P data products";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }


}
