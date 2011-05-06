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

package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * The BEAM reader plugin for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class OsiProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The format name.
     */
    public static final String FORMAT_NAME = "OSI-SAF";
    /**
     * The FIle extension.
     */
    public static final String FILE_EXTENSION_HDF = ".hdf";

    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        final File file;
        if (o instanceof File) {
            file = (File) o;
        } else {
            file = new File(o.toString());
        }
        if (!file.getName().endsWith(FILE_EXTENSION_HDF)) {
            return DecodeQualification.UNABLE;
        }
        final String fileName = file.getName();
        if (!fileName.startsWith("ice_conc_") && !fileName.startsWith("ice_edge_") &&
            !fileName.startsWith("ice_type_") && !fileName.startsWith("multi_conc_") &&
            !fileName.startsWith("multi_edge_") && !fileName.startsWith("multi_type_")) {
            return DecodeQualification.UNABLE;
        }
        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFile.open(file.getAbsolutePath());
            if (netcdfFile.findVariable("Header") != null) {
                return DecodeQualification.INTENDED;
            }
        } catch (Exception ignored) {
        } finally {
            if (netcdfFile != null) {
                try {
                    netcdfFile.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{File.class, String.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new OsiProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION_HDF};
    }

    @Override
    public String getDescription(Locale locale) {
        return "A BEAM reader for Ocean & Sea Ice Science Application Facility (OSI SAF) data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }
}
