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
public class NcAvhrrGacProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "AVHRR-GAC-NC";
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
        return filename.matches("[0-9]{14}-AVHRR-L1b-AVHRR.._G-v02\\.0-fv..\\..*\\.nc");
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new NcAvhrrGacProductReader(this);
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
        return "Converted AVHRR-GAC L1b data products.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

}
