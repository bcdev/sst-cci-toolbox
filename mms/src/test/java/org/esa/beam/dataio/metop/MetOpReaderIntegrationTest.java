package org.esa.beam.dataio.metop;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(IoTestRunner.class)
public class MetOpReaderIntegrationTest {

    @Test
    public void testReadProduct() throws IOException {
        final File testDataDirectory = TestUtil.getTestDataDirectory();
        final File file = new File(testDataDirectory, "AVHR_xxx_1B_M02_20080211161603Z_20080211175803Z_N_O_20080211175632Z.nat");
        final MetopReaderPlugIn metopReaderPlugIn = new MetopReaderPlugIn();
        final DecodeQualification decodeQualification = metopReaderPlugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);


        final MetopReader metopReader = new MetopReader(metopReaderPlugIn);
        final Product product = metopReader.readProductNodes(file, null);
        assertNotNull(product);
        try {
            final ProductData.UTC startTime = product.getStartTime();
            assertNotNull(startTime);
            assertEquals(1202746563080L, startTime.getAsDate().getTime());

            final ProductData.UTC endTime = product.getEndTime();
            assertNotNull(endTime);
            assertEquals(1202752683082L, endTime.getAsDate().getTime());

            assertTiePointValue("longitude", 10, 10 , -123.30519104003906,  product);
            assertTiePointValue("latitude", 20, 20 , 79.19400024414062,  product);

            assertPixelValue("reflec_1", 30, 30, 0.0019087587716057897, product);
            assertPixelValue("reflec_2", 40, 40, 0.0030889855697751045, product);
            assertPixelValue("reflec_3a", 50, 50, 0.0, product);
            assertPixelValue("temp_3b", 60, 60, 242.97332763671875, product);
            assertPixelValue("temp_4", 70, 70, 249.91310119628906, product);
            assertPixelValue("temp_5", 80, 80, 250.7367706298828, product);

            assertTiePointValue("sun_zenith", 90, 90 , 96.77499389648438,  product);
            assertTiePointValue("view_zenith", 100, 100 , 59.54999923706055,  product);
            assertTiePointValue("sun_azimuth", 110, 110 , 134.47000122070312,  product);
            assertTiePointValue("view_azimuth", 120, 120 , 87.81999969482422,  product);
        } finally {
            product.dispose();
        }
    }

    private void assertTiePointValue(String gridName, int x, int y, double expected, Product product) {
        final TiePointGrid latitude = product.getTiePointGrid(gridName);
        assertNotNull(latitude);
        assertEquals(expected, latitude.getPixelDouble(x, y), 1e-8);
    }

    private void assertPixelValue(String bandName, int x, int y, double expected, Product product) throws IOException {
        final double[] doubles = new double[1];
        final Band band = product.getBand(bandName);
        assertNotNull(band);
        band.readPixels(x, y, 1, 1, doubles);
        assertEquals(expected, doubles[0] , 1e-8);
    }
}
