package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Thomas Storm
 */
public class OsiProductReaderPlugInTest {

    private OsiProductReaderPlugIn plugin;

    @Test
    public void testGetDecodeQualification_ForSeaIceConcentrationFile() throws Exception {
        assertEquals(DecodeQualification.INTENDED,
                     plugin.getDecodeQualification(getIceConcentrationFile()));
        assertEquals(DecodeQualification.INTENDED,
                     plugin.getDecodeQualification(getIceConcentrationFile().getPath()));
        assertEquals(DecodeQualification.UNABLE,
                     plugin.getDecodeQualification("ice_conc_sh_qual_201006301200.nc"));
    }

    @Test
    public void testGetDecodeQualification_ForSeaIceConcentrationQualityFile() throws Exception {
        assertEquals(DecodeQualification.INTENDED,
                     plugin.getDecodeQualification(getIceConcentrationQualityFile()));
        assertEquals(DecodeQualification.INTENDED,
                     plugin.getDecodeQualification(getIceConcentrationQualityFile().getPath()));
        assertEquals(DecodeQualification.UNABLE,
                     plugin.getDecodeQualification("some_file.hdf"));
        assertEquals(DecodeQualification.UNABLE,
                     plugin.getDecodeQualification(new File("ice_conc_sh_qual_201006301200.nc")));
    }

    @Test
    public void testGetDecodeQualification_ForUnspecificHdfFile() throws Exception {
        assertEquals(DecodeQualification.UNABLE,
                     plugin.getDecodeQualification("any.hdf"));
    }

    @Test
    public void testGetDecodeQualification_ForNetcdfFile() throws Exception {
        assertEquals(DecodeQualification.UNABLE,
                     plugin.getDecodeQualification(new File("ice_conc_sh_qual_201006301200.nc")));
    }

    @Test
    public void testGetInputTypes() throws Exception {
        Class[] inputTypes = plugin.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(File.class, inputTypes[0]);
        assertEquals(String.class, inputTypes[1]);
    }

    @Test
    public void testCreateReaderInstance() throws Exception {
        final ProductReader reader = plugin.createReaderInstance();
        assertNotNull(reader);
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertEquals(1, plugin.getFormatNames().length);
        assertEquals("OSI-SAF", plugin.getFormatNames()[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() throws Exception {
        assertEquals(1, plugin.getDefaultFileExtensions().length);
        assertEquals(".hdf", plugin.getDefaultFileExtensions()[0]);
    }

    @Test
    public void testGetDescription() throws Exception {
        assertNotNull(plugin.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() throws Exception {
        assertNotNull(plugin.getProductFileFilter());
    }

    @Before
    public void init() throws Exception {
        plugin = new OsiProductReaderPlugIn();
    }

    private static File getIceConcentrationFile() throws URISyntaxException {
        return getResourceAsFile("ice_conc_nh_201006301200.hdf");
    }

    private static File getIceConcentrationQualityFile() throws URISyntaxException {
        return getResourceAsFile("ice_conc_sh_qual_201006301200.hdf");
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        return new File(OsiProductReaderPlugInTest.class.getResource(name).toURI());
    }

}
