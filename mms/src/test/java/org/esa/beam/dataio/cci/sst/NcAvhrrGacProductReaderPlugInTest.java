package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 */
@SuppressWarnings("InstanceofInterfaces")
public class NcAvhrrGacProductReaderPlugInTest {

    private NcAvhrrGacProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new NcAvhrrGacProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        final File file = new File("19910101000100-ESACCI-L1C-AVHRR11_G-fv01.0.nc");

        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("20100601.egr"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("aai_20101224.nc"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("ice_conc_nh_201006301200.hdf"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("ice_conc_sh_qual_201006301200.hdf"));
    }

    @Test
    public void testMatches() throws Exception {
        assertTrue(NcAvhrrGacProductReaderPlugIn.matches("19910101000100-ESACCI-L1C-AVHRR11_G-fv01.0.nc"));
        assertTrue(NcAvhrrGacProductReaderPlugIn.matches("20061031223900-ESACCI-L1C-AVHRRMTA_G-fv01.0.nc"));
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader reader = plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof NcAvhrrGacProductReader);
    }

    @Test
    public void testGteFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("AVHRR-GAC-NC", formatNames[0]);
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testGetFileExtension() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".nc", defaultFileExtensions[0]);
    }

    @Test
    public void testGetDescription() {
         assertEquals("SST-CCI AVHRR-GAC L1c data products", plugIn.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() {
        final BeamFileFilter filter = plugIn.getProductFileFilter();
        assertNotNull(filter);
        assertEquals("AVHRR-GAC-NC",filter.getFormatName());
    }
}
