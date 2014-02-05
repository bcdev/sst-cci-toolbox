package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EgrAaiProductReaderPluginTest {

    private EgrAaiProductReaderPlugIn plugIn;

    @Before
    public void setUp(){
        plugIn = new EgrAaiProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() {
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification("20120623.egr"));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(new File("20140130.egr")));

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(new File("A20140130.egr")));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("20140130.docx"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("a certain file"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(""));
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertEquals(2, inputTypes.length);

        assertEquals(File.class, inputTypes[0]);
        assertEquals(String.class, inputTypes[1]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();

        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".egr", defaultFileExtensions[0]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugIn.getDescription(null);

        assertEquals("Metop-A/GOME-2 Absorbing Aerosol Index", description);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertEquals(1, formatNames.length);
        assertEquals("AAI-EGR", formatNames[0]);
    }

    @Test
    public void testGetProductFileFilter() {
        final BeamFileFilter fileFilter = plugIn.getProductFileFilter();
        assertNotNull(fileFilter);

        assertEquals(".egr", fileFilter.getDefaultExtension());
        assertEquals("AAI-EGR", fileFilter.getFormatName());
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugIn.createReaderInstance();

        assertNotNull(readerInstance);
        assertTrue(readerInstance instanceof EgrAaiProductReader);
    }
}
