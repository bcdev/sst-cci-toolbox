package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.TestHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

// @todo 1 tb/tb make inherit from iotestclass tb 2014-06-03
public class NcAvhrrGacProductReaderIntegrationTest {

    private NcAvhrrGacProductReader reader;

    @Before
    public void setUp() {
        reader = new NcAvhrrGacProductReader(new NcAvhrrGacProductReaderPlugIn());
    }

    @Test
    public void testReadL1B_11_TestProduct() throws Exception {
        final String path = TestHelper.getResourcePath(NcAvhrrGacProductReaderIntegrationTest.class, "19940101000100-AVHRR-L1b-AVHRR11_G-v02.0-fv01.0.nc");
        assertNotNull(path);

        final Product product = reader.readProductNodes(path, null);
        assertNotNull(product);
        try {
            final Band ch1Band = product.getBand("ch1");
            assertNotNull(ch1Band);
            // @todo 1 tb/tb continue here tb 2014-06-03
            //assertEquals(-32768.f, ch1Band.getPixelFloat(1,1), 1e-8);

            final Band ch2Band = product.getBand("ch2");
            assertNotNull(ch2Band);

            final Band ch3bBand = product.getBand("ch3b");
            assertNotNull(ch3bBand);

            final Band ch4Band = product.getBand("ch4");
            assertNotNull(ch4Band);

            final Band ch5Band = product.getBand("ch5");
            assertNotNull(ch5Band);

            final Band cloudMaskBand = product.getBand("cloud_mask");
            assertNotNull(cloudMaskBand);

            final Band cloudProbabilityBand = product.getBand("cloud_probability");
            assertNotNull(cloudProbabilityBand);

            final Band dtimeBand = product.getBand("dtime");
            assertNotNull(dtimeBand);

            final Band latBand = product.getBand("lat");
            assertNotNull(latBand);

            final Band lonBand = product.getBand("lon");
            assertNotNull(lonBand);

            final Band qualFlagsBand = product.getBand("qual_flags");
            assertNotNull(qualFlagsBand);

            final Band relativeAzimuthAngleBand = product.getBand("relative_azimuth_angle");
            assertNotNull(relativeAzimuthAngleBand);

            final Band satelliteZenithAngleBand = product.getBand("satellite_zenith_angle");
            assertNotNull(satelliteZenithAngleBand);

            final Band solarZenithAngleBand = product.getBand("solar_zenith_angle");
            assertNotNull(solarZenithAngleBand);
        } finally {
            product.dispose();
        }
    }
}
