package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class AmsreProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String AMSRE_FILE_NAME_PATTERN = "AMSR_E_L2A_BrightnessTemperatures_V[0-9]{2}_[0-9]{12}_[a-zA-Z].hdf";
    private static final String FORMAT_NAME = "AMSRE_L2A";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file;
        if (input instanceof File) {
            file = (File) input;
        } else if (input instanceof String) {
            file = new File((String) input);
        } else {
            return DecodeQualification.UNABLE;
        }

        if (!isCorrectFilename(file.getName())) {
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
        return new AmsreProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".hdf"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "AMSRE Level 2a data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

    static boolean isCorrectFilename(String fileName) {
        return fileName.matches(AMSRE_FILE_NAME_PATTERN);
    }
}
