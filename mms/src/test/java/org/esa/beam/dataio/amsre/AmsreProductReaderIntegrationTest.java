package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
    IMPORTANT!
    ---------
    This class in NOT supporting all bands in all resolutions. The bands and resolutions available through the Product
    interface are restricted to the ones requested by the SST-CCI scientific partners.
    tb 2016-07-27
 */

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

            final ProductData.UTC startTime = product.getStartTime();
            assertNotNull(startTime);
            assertEquals(1108615594000L, startTime.getAsDate().getTime());

            final ProductData.UTC endTime = product.getEndTime();
            assertEquals(1108618596000L, endTime.getAsDate().getTime());
            assertNotNull(endTime);

            assertBandCorrect("Time", ProductData.TYPE_FLOAT64, product);
            assertBandCorrect("Latitude", ProductData.TYPE_FLOAT32, product);
            assertBandCorrect("Longitude", ProductData.TYPE_FLOAT32, product);

            assertBandCorrect("89.0V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("89.0H_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("36.5V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("36.5H_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("23.8V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("23.8H_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("18.7V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("18.7H_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("10.7V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("10.7H_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("6.9V_Res.1_TB", ProductData.TYPE_INT16, product);
            assertBandCorrect("6.9H_Res.1_TB", ProductData.TYPE_INT16, product);

            assertBandCorrect("Sun_Elevation", ProductData.TYPE_INT16, product);
            assertBandCorrect("Sun_Azimuth", ProductData.TYPE_INT16, product);
            assertBandCorrect("Earth_Incidence", ProductData.TYPE_INT16, product);
            assertBandCorrect("Earth_Azimuth", ProductData.TYPE_INT16, product);

            // @todo 1 tb/tb verify with partners - seems to be a 2D dataset with flag and channel dimension 2016-07-27
            assertBandCorrect("Channel_Quality_Flag_6_To_52", ProductData.TYPE_INT16, product);

            // @todo 1 tb/tb verify with partners - which dimensions? 2016-07-27
            assertBandCorrect("Resampled_Channel_Quality_Flag", ProductData.TYPE_INT16, product);

            // @todo 1 tb/tb verify with partners - seems to be a 3D dataset with flag and channel dimension 2016-07-27
            assertBandCorrect("Land_Ocean_Flag_for_6_10_18_23_36_50_89A", ProductData.TYPE_UINT8, product);

            assertBandCorrect("Res1_Surf", ProductData.TYPE_UINT8, product);

            // @todo 1 tb/tb add tests for geocoding 2016-07-27
//            final GeoCoding geoCoding = product.getGeoCoding();
//            assertNotNull(geoCoding);
        } finally {
            product.dispose();
        }
    }

    private void assertBandCorrect(String bandName, int dataType, Product product) {
        final Band band = product.getBand(bandName);
        assertNotNull(band);
        assertEquals(dataType, band.getDataType());
    }
}
