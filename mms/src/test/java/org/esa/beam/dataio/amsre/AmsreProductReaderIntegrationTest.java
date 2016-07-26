package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(IoTestRunner.class)
public class AmsreProductReaderIntegrationTest {

    @Test
    public void testReadProduct_v12() throws IOException {
        final File file = TestUtil.getFileInTestDataDirectory("AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A.hdf");

        final AmsreProductReaderPlugIn readerPlugIn = new AmsreProductReaderPlugIn();
        final DecodeQualification decodeQualification = readerPlugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);

        final AmsreProductReader productReader = new AmsreProductReader(readerPlugIn);
        final Product product = productReader.readProductNodes(file, null);
        assertNotNull(product);

        try {
            assertEquals("AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A", product.getName());
            assertEquals("AMSRE_L2A", product.getProductType());
            assertEquals(243, product.getSceneRasterWidth());
            assertEquals(2002, product.getSceneRasterHeight());
        } finally {
            product.dispose();
        }
    }
}
