package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(IoTestRunner.class)
public class NcAvhrrGacProductReaderIntegrationTest {

    private NcAvhrrGacProductReader reader;

    @Before
    public void setUp() {
        reader = new NcAvhrrGacProductReader(new NcAvhrrGacProductReaderPlugIn());
    }

    @Test
    public void testReadL1B_11_TestProduct() throws Exception {
        final String path = TestHelper.getResourcePath(NcAvhrrGacProductReaderIntegrationTest.class, "19890205073700-ESACCI-L1C-AVHRR10_G-fv01.0.nc");
        assertNotNull(path);

        final Product product = reader.readProductNodes(path, null);
        assertNotNull(product);
        try {
            assertBandCorrect("ch1", 1, 1, 0.0, 1e-4, 0.0, "reflectance", product);
            assertBandCorrect("ch2", 2, 2, 0.002499999936844688, 1e-4, 0.0, "reflectance", product);
            assertBandCorrect("ch3b", 3, 3, -54.529998779296875, 0.01, 273.1499938964844, "kelvin", product);
            assertBandCorrect("ch4", 4, 4, 233.56999478116632, 0.01, 273.1499938964844, "kelvin", product);
            assertBandCorrect("ch5", 5, 5, -54.53, 0.01, 273.15, "kelvin", product);

            assertCloudMaskBandCorrect(product);

            assertBandCorrect("cloud_probability", 7, 7, 128, 1.0, 0.0, null, product);
            assertBandCorrect("dtime", 8, 8, 0.0, 0.01, 0.0, "seconds", product);
            assertBandCorrect("ict_temp", 9, 9, 297.49999335221946, 0.01, 273.1499938964844, "kelvin", product);


            assertBandCorrect("lat", 9, 9, 48.35000228881836, 1.0, 0.0, "degrees_north", product);
            assertBandCorrect("lon", 10, 10, -173.11300659179688, 1.0, 0.0, "degrees_east", product);

            assertCorrectFlagsBand(product);

            assertBandCorrect("relative_azimuth_angle", 12, 12, -327.67999267578125, 0.01, 0.0, "angular_degree", product);
            assertBandCorrect("satellite_zenith_angle", 13, 13, 61.869998617097735, 0.01, 0.0, "angular_degree", product);
            assertBandCorrect("solar_zenith_angle", 14, 14, 119.24999733455479, 0.01, 0.0, "angular_degree", product);

        } finally {
            product.dispose();
        }
    }

    @Test
    public void testReadL1B_v_1_2_TestProduct() throws Exception {
        final String path = TestHelper.getResourcePath(NcAvhrrGacProductReaderIntegrationTest.class, "19830601031700-ESACCI-L1C-AVHRR07_G-fv01.0.nc");
        assertNotNull(path);

        final Product product = reader.readProductNodes(path, null);
        assertNotNull(product);

        try {
            assertBandCorrect("ch1", 1, 1, 0.2635999933409039, 1e-4, 0.0, "reflectance", product);
            assertBandCorrect("ch2", 2, 2, 0.20239999488694593, 1e-4, 0.0, "reflectance", product);
            assertBandCorrect("ch3b", 3, 3, 289.92999352142215, 0.01, 273.1499938964844, "kelvin", product);
            assertBandCorrect("ch4", 4, 4, 269.36999398097396, 0.01, 273.1499938964844, "kelvin", product);
            assertBandCorrect("ch5", 5, 5, 269.6799939740449, 0.01, 273.1499938964844, "kelvin", product);

            assertCloudMaskBandCorrect(product);

            assertBandCorrect("cloud_probability", 5, 5, -0.00393599271774292, 0.0039369999431073666, 0.5, null, product);
            assertBandCorrect("dtime", 6, 6, 3.000640869140625, 1.0, 0.0, "seconds", product);
            assertBandCorrect("ict_temp", 7, 7, 288.7099935486913, 0.01, 273.1499938964844, "kelvin", product);

            assertBandCorrect("lat", 7, 7, 47.566001892089844, 1.0, 0.0, "degrees_north", product);
            assertBandCorrect("lon", 8, 8, -174.86199951171875, 1.0, 0.0, "degrees_east", product);

            assertCorrectFlagsBand(product);

            assertBandCorrect("relative_azimuth_angle", 10, 10, -327.67999267578125, 0.01, 0.0, "angular_degree", product);
            assertBandCorrect("satellite_zenith_angle", 11, 11, 63.6699985768646, 0.01, 0.0, "angular_degree", product);
            assertBandCorrect("solar_zenith_angle", 12, 12, 50.019998881965876, 0.01, 0.0, "angular_degree", product);
        } finally {
            product.dispose();
        }
    }


    private void assertCorrectFlagsBand(Product product) {
        final Band qualFlagsBand = product.getBand("qual_flags");
        assertNotNull(qualFlagsBand);
        assertEquals(0.0, ProductUtils.getGeophysicalSampleDouble(qualFlagsBand, 11, 11, 0), 1e-8);
        final FlagCoding flagCoding = qualFlagsBand.getFlagCoding();
        assertNotNull(flagCoding);
        final String[] flagNames = flagCoding.getFlagNames();
        assertArrayEquals(new String[]{"bad_navigation", "bad_calibration", "bad_timing", "missing_line", "bad_data"}, flagNames);
        assertEquals(1, flagCoding.getFlagMask("bad_navigation"));
        assertEquals(2, flagCoding.getFlagMask("bad_calibration"));
        assertEquals(4, flagCoding.getFlagMask("bad_timing"));
        assertEquals(8, flagCoding.getFlagMask("missing_line"));
        assertEquals(16, flagCoding.getFlagMask("bad_data"));
    }

    private void assertBandCorrect(String name, int x, int y, double value, double scaleFactor, double scaleOffset, String unit, Product product) {
        final Band band = product.getBand(name);
        assertNotNull(band);
        assertEquals(scaleFactor, band.getScalingFactor(), 1e-8);
        assertEquals(scaleOffset, band.getScalingOffset(), 1e-8);
        assertEquals(unit, band.getUnit());
        assertEquals(value, ProductUtils.getGeophysicalSampleDouble(band, x, y, 0), 1e-8);
    }

    private void assertCloudMaskBandCorrect(Product product) {
        final Band cloudMaskBand = product.getBand("cloud_mask");
        assertNotNull(cloudMaskBand);
        assertEquals(7.0, ProductUtils.getGeophysicalSampleDouble(cloudMaskBand, 6, 6, 0), 1e-8);
        final IndexCoding indexCoding = cloudMaskBand.getIndexCoding();
        assertNotNull(indexCoding);
        final String[] indexNames = indexCoding.getIndexNames();
        assertNotNull(indexNames);
        assertArrayEquals(new String[]{"clear", "probably_clear", "probably_cloudy", "cloudy", "unprocessed"}, indexNames);
        assertEquals(0, indexCoding.getIndexValue("clear"));
        assertEquals(1, indexCoding.getIndexValue("probably_clear"));
        assertEquals(2, indexCoding.getIndexValue("probably_cloudy"));
        assertEquals(3, indexCoding.getIndexValue("cloudy"));
        assertEquals(7, indexCoding.getIndexValue("unprocessed"));
    }
}
