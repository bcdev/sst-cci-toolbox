package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(IoTestRunner.class)
public class AmsreProductReaderPlugInIOTest {

    private AmsreProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new AmsreProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws IOException {
        final File testDataDirectory = TestUtil.getTestDataDirectory();

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(12.6));

        final File incorrectFile = new File(testDataDirectory, "AVHR_xxx_1B_M02_20080211161603Z_20080211175803Z_N_O_20080211175632Z.nat");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(incorrectFile));

        final String incorrectPath = incorrectFile.getAbsolutePath();
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(incorrectPath));

        final File correctFile = new File(testDataDirectory, "AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A.hdf");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(correctFile));
    }
}
