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
 * Product reader plug-in for TMI and AMSR-E products.
 *
 * @author Ralf Quast
 */
public class PmwProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The AMSR-E product file name pattern.
     */
    public static final String AMS_FILE_NAME_PATTERN = "[0-9]*-AMSRE-REMSS-L2P-amsr_.*\\.nc";
    /**
     * The TMI product file name pattern.
     */
    public static final String TMI_FILE_NAME_PATTERN = "[0-9]*-TMI-REMSS-L2P-tmi_.*\\.nc";
    /**
     * The format name.
     */
    public static final String FORMAT_NAME = "PMW";
    /**
     * The file extension.
     */
    public static final String FILE_EXTENSION_NC = ".nc";

    @Override
    public DecodeQualification getDecodeQualification(Object o) {
        final File file;
        if (o instanceof File) {
            file = (File) o;
        } else {
            file = new File(o.toString());
        }
        if (!file.getName().matches(AMS_FILE_NAME_PATTERN) && !file.getName().matches(TMI_FILE_NAME_PATTERN)) {
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
        return new PmwProductReader(this);
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
        return "A BEAM reader for passive microwave sensor (AMSR-E and TMI) data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

}
