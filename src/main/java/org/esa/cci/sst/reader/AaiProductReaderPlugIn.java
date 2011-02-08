package org.esa.cci.sst.reader;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class AaiProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public AaiProductReader createReaderInstance() {
        return new AaiProductReader(this);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = input instanceof File ? (File) input : new File(input.toString());
        if (file.getName().endsWith(".egr")) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{File.class, String.class};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".egr"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "Metop-A/GOME-2 Absorbing Aerosol Index";
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"AAI-EGR"};
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
