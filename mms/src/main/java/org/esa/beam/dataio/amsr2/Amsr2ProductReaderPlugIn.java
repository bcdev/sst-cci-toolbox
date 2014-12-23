/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.amsr2;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Quast
 */
public class Amsr2ProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The AMSR2 product file name pattern.
     */
    public static final String AMSR2_FILE_NAME_PATTERN = "GW1AM2_[0-9]{12}_...._L1SGRTBR_[0-9]{7}\\.h5";
    /**
     * The format name.
     */
    public static final String FORMAT_NAME = "AMSR2_L1R";
    /**
     * The file extension.
     */
    public static final String FILE_EXTENSION_H5 = ".h5";

    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        final File file;
        if (o instanceof File) {
            file = (File) o;
        } else if (o instanceof String) {
            file = new File((String) o);
        } else {
            return DecodeQualification.UNABLE;
        }
        if (!file.getName().matches(AMSR2_FILE_NAME_PATTERN)) {
            return DecodeQualification.UNABLE;
        }
        try {
            if (NetcdfFile.canOpen(file.getPath())) {
                return DecodeQualification.INTENDED;
            }
        } catch (IOException ignored) {
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{File.class, String.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new Amsr2ProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION_H5};
    }

    @Override
    public String getDescription(Locale locale) {
        return "AMSR2 Level-1R data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }
}
