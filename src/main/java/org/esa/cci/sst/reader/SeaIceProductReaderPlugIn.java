package org.esa.cci.sst.reader;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.util.Locale;

/**
 * The BEAM reader plugin for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class SeaIceProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String FORMAT_NAME = "Ocean and Sea Ice SAF";
    private static final String FILE_EXTENSION_HDF = ".hdf";

    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        if (!(o instanceof File) && !(o instanceof String)) {
            return DecodeQualification.UNABLE;
        }
        File file = new File(o.toString());
        if (!FILE_EXTENSION_HDF.equals(FileUtils.getExtension(file))) {
            return DecodeQualification.UNABLE;
        }
        final String fileName = file.getName();
        if (!fileName.startsWith("ice_conc_") && !fileName.startsWith("ice_edge_") &&
            !fileName.startsWith("ice_type_") && !fileName.startsWith("multi_conc_") &&
            !fileName.startsWith("multi_edge_") && !fileName.startsWith("multi_type_")) {
            return DecodeQualification.UNABLE;
        }
        try {
            if (!NetcdfFile.canOpen(file.getAbsolutePath())) {
                return DecodeQualification.UNABLE;
            }
            final NetcdfFile netcdfFile = NetcdfFile.open(file.getAbsolutePath());
            final Variable header = netcdfFile.findVariable("Header");
            if(header == null) {
                return DecodeQualification.UNABLE;
            }
        } catch (Exception ignore) {
            return DecodeQualification.UNABLE;
        }

        return DecodeQualification.INTENDED;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{File.class, String.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new SeaIceProductReader(this);
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
        return "A BEAM reader for Ocean & Sea Ice SAF data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }
}
