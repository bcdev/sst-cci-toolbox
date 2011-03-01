package org.esa.cci.sst.reader;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SeaIceProductReaderPlugInTest {

    private SeaIceProductReaderPlugIn plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new SeaIceProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification("testdata/SeaIceConc/ice_conc_sh_qual_201006301200.hdf"));
        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification(new File("testdata/SeaIceConc/ice_conc_sh_qual_201006301200.hdf")));
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification(new File("testdata/SeaIceConc/some_hdf_file.hdf")));
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification(new File("testdata/SeaIceConc/ice_conc_sh_qual_201006301200.nc")));
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
        assertEquals("Ocean and Sea Ice SAF", plugin.getFormatNames()[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() throws Exception {
        assertEquals(1, plugin.getDefaultFileExtensions().length);
        assertEquals(".hdf", plugin.getDefaultFileExtensions()[0]);
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("A BEAM reader for Ocean & Sea Ice SAF data products.", plugin.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() throws Exception {
        assertNotNull(plugin.getProductFileFilter());
    }
}
