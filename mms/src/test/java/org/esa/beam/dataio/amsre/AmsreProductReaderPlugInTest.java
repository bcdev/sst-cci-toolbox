package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.cci.sst.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AmsreProductReaderPlugInTest {

    private AmsreProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new AmsreProductReaderPlugIn();
    }

    @Test
    public void testgetInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertEquals(2, inputTypes.length);

        assertEquals(File.class, inputTypes[0]);
        assertEquals(String.class, inputTypes[1]);
    }

    @Test
    public void testIsCorrectFileName() {
         assertTrue(AmsreProductReaderPlugIn.isCorrectFilename("AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A.hdf"));
         assertTrue(AmsreProductReaderPlugIn.isCorrectFilename("AMSR_E_L2A_BrightnessTemperatures_V12_200502172026_D.hdf"));

        assertFalse(AmsreProductReaderPlugIn.isCorrectFilename("AMSR_E_L2A_BrightnessTemperatures_V12_200502170307_A.hdf.xml"));
        assertFalse(AmsreProductReaderPlugIn.isCorrectFilename("AMSR_E_L2A_BrightnessTemperatures_V12_200502170307_A.ph"));
        assertFalse(AmsreProductReaderPlugIn.isCorrectFilename("AMSR_E_L2A_BrightnessTemperatures_V12_20050217_A_brws.2.jpg"));
    }

    @Test
    public void testCreateReaderInstace() {
        final ProductReader reader = plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof AmsreProductReader);
    }

    @Test
    public void testGetFormatNames(){
        final String[] formatNames = plugIn.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("AMSRE_L2A", formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions(){
        final String[] extensions = plugIn.getDefaultFileExtensions();
        assertEquals(1, extensions.length);
        assertEquals(".hdf", extensions[0]);
    }

    @Test
    public void testGetDescription() {
         assertEquals("AMSRE Level 2a data products.", plugIn.getDescription(null));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter fileFilter = plugIn.getProductFileFilter();
        assertNotNull(fileFilter);
        assertEquals(".hdf", fileFilter.getDefaultExtension());
    }
}
