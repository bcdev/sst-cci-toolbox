package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.*;
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

            assertBandCorrect("Time", ProductData.TYPE_FLOAT64, 243, 2002, product);
            assertBandCorrect("Latitude", ProductData.TYPE_FLOAT32, 243, 2002, product);
            assertBandCorrect("Longitude", ProductData.TYPE_FLOAT32, 243, 2002, product);

            assertBandCorrect("89.0V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("89.0H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("36.5V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("36.5H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("23.8V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("23.8H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("18.7V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("18.7H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("10.7V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("10.7H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("6.9V_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("6.9H_Res.1_TB", ProductData.TYPE_INT16, 243, 2002, product);

            assertBandCorrect("Sun_Elevation", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Sun_Azimuth", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Earth_Incidence", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Earth_Azimuth", ProductData.TYPE_INT16, 243, 2002, product);

            // @todo 1 tb/tb verify with partners - seems to be a 2D dataset with flag and channel dimension 2016-07-27
            assertBandCorrect("Channel_Quality_Flag_6_To_52", ProductData.TYPE_INT16, 243, 2002, product);

            // @todo 1 tb/tb verify with partners - which dimensions? 2016-07-27
            assertBandCorrect("Resampled_Channel_Quality_Flag", ProductData.TYPE_INT16, 243, 2002, product);

            // @todo 1 tb/tb verify with partners - seems to be a 3D dataset with flag and channel dimension 2016-07-27
            assertBandCorrect("Land_Ocean_Flag_for_6_10_18_23_36_50_89A", ProductData.TYPE_UINT8, 243, 2002, product);

            assertBandCorrect("Res1_Surf", ProductData.TYPE_UINT8, 243, 2002, product);

            assertCorrectBandData("Latitude", 5, 277, -65.76707458496094, product);
            assertCorrectBandData("Longitude", 6, 278, 168.5465087890625, product);

            assertCorrectBandData("Time", 7, 279, 3.8276961829433537E8, product);

            assertCorrectBandData("89.0V_Res.1_TB", 8, 280, -8465, product);
            assertCorrectBandData("89.0H_Res.1_TB", 9, 281, -11009, product);
            assertCorrectBandData("36.5V_Res.1_TB", 10, 282, -11018, product);
            assertCorrectBandData("36.5H_Res.1_TB", 11, 283, -16584, product);
            assertCorrectBandData("23.8V_Res.1_TB", 12, 284, -12024, product);
            assertCorrectBandData("23.8H_Res.1_TB", 13, 285, -17721, product);
            assertCorrectBandData("18.7V_Res.1_TB", 14, 286, -13989, product);
            assertCorrectBandData("18.7H_Res.1_TB", 15, 287, -20998, product);
            assertCorrectBandData("10.7V_Res.1_TB", 16, 288, -16153, product);
            assertCorrectBandData("10.7H_Res.1_TB", 17, 289, -23928, product);
            assertCorrectBandData("6.9V_Res.1_TB", 18, 290, -16992, product);
            assertCorrectBandData("6.9H_Res.1_TB", 19, 291, -24796, product);
            assertCorrectBandData("Sun_Elevation", 20, 292, 90, product);
            assertCorrectBandData("Sun_Azimuth", 21, 293, 888, product);
            assertCorrectBandData("Earth_Incidence", 22, 294, 11019, product);
            assertCorrectBandData("Earth_Azimuth", 23, 295, 2629, product);
            // @todo 1 tb/tb add tests when format has been agreed on 2016-07-27
            //assertCorrectBandData("Channel_Quality_Flag_6_To_52", 23, 295, 2629, product);
            //assertCorrectBandData("Resampled_Channel_Quality_Flag", 23, 295, 2629, product);
            //assertCorrectBandData("Land_Ocean_Flag_for_6_10_18_23_36_50_89A", 23, 295, 2629, product);
            assertCorrectBandData("Res1_Surf", 27, 299, 0, product);

            final GeoCoding geoCoding = product.getGeoCoding();
            assertNotNull(geoCoding);
            final PixelPos pixelPos = new PixelPos(100.5f, 100.5f);
            final GeoPos geoPos = new GeoPos();
            geoCoding.getGeoPos(pixelPos, geoPos);
            assertEquals(-178.33155822753906, geoPos.getLon(), 1e-8);
            assertEquals(-77.40758514404297, geoPos.getLat(), 1e-8);
            pixelPos.setLocation(200.5f, 200.5f);
            geoCoding.getGeoPos(pixelPos, geoPos);
            assertEquals(137.9962615966797, geoPos.getLon(), 1e-8);
            assertEquals(-75.54114532470703, geoPos.getLat(), 1e-8);
        } finally {
            product.dispose();
        }
    }

    private void assertCorrectBandData(String bandName, int x, int y, double expected, Product product) throws IOException {
        final Band band = product.getBand(bandName);
        final double[] doubles = new double[1];
        band.readPixels(x, y, 1, 1, doubles);
        assertEquals(expected, doubles[0], 1e-8);
    }

    private void assertBandCorrect(String bandName, int dataType, int width, int height, Product product) {
        final Band band = product.getBand(bandName);
        assertNotNull(band);
        assertEquals(dataType, band.getDataType());
        assertEquals(width, band.getRasterWidth());
        assertEquals(height, band.getRasterHeight());
    }
}
