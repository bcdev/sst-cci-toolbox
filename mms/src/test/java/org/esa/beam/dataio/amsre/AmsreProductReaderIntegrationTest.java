package org.esa.beam.dataio.amsre;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
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

            assertBandCorrect("Time", ProductData.TYPE_FLOAT64, 243, 2002, product);
            assertBandCorrect("Latitude", ProductData.TYPE_FLOAT32, 243, 2002, product);
            assertBandCorrect("Longitude", ProductData.TYPE_FLOAT32, 243, 2002, product);
            //
            assertBandCorrect("89_0V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("89_0H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("36_5V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("36_5H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("23_8V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("23_8H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("18_7V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("18_7H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("10_7V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("10_7H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("6_9V_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("6_9H_Res_1_TB", ProductData.TYPE_INT16, 243, 2002, product);
            //
            assertBandCorrect("Sun_Elevation", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Sun_Azimuth", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Earth_Incidence", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Earth_Azimuth", ProductData.TYPE_INT16, 243, 2002, product);
            // not checking all flag bands here tb 2016-07-29
            assertBandCorrect("Channel_Quality_Flag_6V", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Channel_Quality_Flag_6H", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Channel_Quality_Flag_18H", ProductData.TYPE_INT16, 243, 2002, product);
            assertBandCorrect("Channel_Quality_Flag_23V", ProductData.TYPE_INT16, 243, 2002, product);
            // only checking the one band used for the MMD tb 2016-07-29
            assertBandCorrect("Land_Ocean_Flag_6", ProductData.TYPE_UINT8, 243, 2002, product);
            //
            assertBandCorrect("Res1_Surf", ProductData.TYPE_UINT8, 243, 2002, product);
            //
            assertBandCorrect("Scan_Quality_Flag", ProductData.TYPE_INT32, 243, 2002, product);
            //
            assertBandCorrect("Solar_Zenith_Angle", ProductData.TYPE_FLOAT32, 243, 2002, product);
            assertBandCorrect("Solar_Azimuth_Angle", ProductData.TYPE_FLOAT32, 243, 2002, product);

            assertCorrectBandData("Latitude", 2, 2, -73.20999145507812, product);
            assertCorrectBandData("Latitude", 240, 2, -83.60804748535156, product);
            assertCorrectBandData("Latitude", 2, 1999, 85.28460693359375, product);
            assertCorrectBandData("Latitude", 240, 1999, 73.81525421142578, product);
            assertCorrectBandData("Longitude", 6, 278, 168.5465087890625, product);
            assertCorrectBandData("Time", 7, 279, 3.8276961829433537E8, product);
            //                                                                              raw counts
            assertCorrectBandData("89_0V_Res_1_TB", 8, 280, 243.0299945678562, product);    // -8465
            assertCorrectBandData("89_0H_Res_1_TB", 9, 281, 217.5899951364845, product);    // -11009
            assertCorrectBandData("36_5V_Res_1_TB", 10, 282, 217.49999513849616, product);  // -11018
            assertCorrectBandData("36_5H_Res_1_TB", 11, 283, 161.8399963825941, product);   // -16584
            assertCorrectBandData("23_8V_Res_1_TB", 12, 284, 207.43999536335468, product);  // -12024
            assertCorrectBandData("23_8H_Res_1_TB", 13, 285, 150.4699966367334, product);   // -17721
            assertCorrectBandData("18_7V_Res_1_TB", 14, 286, 187.7899958025664, product);   // -13989
            assertCorrectBandData("18_7H_Res_1_TB", 15, 287, 117.69999736919999, product);  // -20998
            assertCorrectBandData("10_7V_Res_1_TB", 16, 288, 166.1499962862581, product);   // -16153
            assertCorrectBandData("10_7H_Res_1_TB", 17, 289, 88.39999802410603, product);   // -23928
            assertCorrectBandData("6_9V_Res_1_TB", 18, 290, 157.75999647378922, product);   // -16992
            assertCorrectBandData("6_9H_Res_1_TB", 19, 291, 79.71999821811914, product);    // -24796
            assertCorrectBandData("Sun_Elevation", 20, 292, 9.00000013411045, product);     // 90
            assertCorrectBandData("Sun_Azimuth", 21, 293, 88.80000132322311, product);      // 888
            assertCorrectBandData("Earth_Incidence", 22, 294, 55.094998768530786, product); // 11019
            assertCorrectBandData("Earth_Azimuth", 23, 295, 26.28999941237271, product);    // 2629
            assertCorrectBandData("Channel_Quality_Flag_6V", 0, 0, 7, product);
            assertCorrectBandData("Channel_Quality_Flag_6H", 1, 1, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_10V", 2, 2, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_10H", 3, 3, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_18V", 4, 4, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_18H", 5, 5, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_23V", 6, 6, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_23H", 7, 7, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_36V", 8, 8, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_36H", 9, 9, 0, product);
            assertCorrectBandData("Channel_Quality_Flag_89V", 10, 10, 11, product);
            assertCorrectBandData("Channel_Quality_Flag_89H", 11, 11, 11, product);
            //
            assertCorrectBandData("Land_Ocean_Flag_6", 8, 12, 69, product);
            assertCorrectBandData("Land_Ocean_Flag_6", 10, 12, 99, product);
            assertCorrectBandData("Land_Ocean_Flag_6", 12, 12, 100, product);
            //
            assertCorrectBandData("Scan_Quality_Flag", 26, 298, 0, product);
            //
            assertCorrectBandData("Res1_Surf", 27, 299, 0, product);
            //
            assertCorrectBandData("Solar_Zenith_Angle", 28, 300, 62.79499816894531, product);
            assertCorrectBandData("Solar_Azimuth_Angle", 29, 301, 120.02999877929688, product);

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
