package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        final String path = TestHelper.getResourcePath(NcAvhrrGacProductReaderIntegrationTest.class, "19890205073700-AVHRR-L1b-AVHRR10_G-v02.0-fv01.0.nc");
        assertNotNull(path);

        final Product product = reader.readProductNodes(path, null);
        assertNotNull(product);
        try {
            final Band ch1Band = product.getBand("ch1");
            assertNotNull(ch1Band);
            assertEquals(1e-4, ch1Band.getScalingFactor(), 1e-8);
            assertEquals("reflectance", ch1Band.getUnit());
            assertEquals(0.0, ProductUtils.getGeophysicalSampleDouble(ch1Band, 1, 1, 0), 1e-8);

            final Band ch2Band = product.getBand("ch2");
            assertNotNull(ch2Band);
            assertEquals(1e-4, ch2Band.getScalingFactor(), 1e-8);
            assertEquals("reflectance", ch2Band.getUnit());
            assertEquals(0.002499999936844688, ProductUtils.getGeophysicalSampleDouble(ch2Band, 2, 2, 0), 1e-8);

            final Band ch3bBand = product.getBand("ch3b");
            assertNotNull(ch3bBand);
            assertEquals(0.01, ch3bBand.getScalingFactor(), 1e-8);
            assertEquals(273.15, ch3bBand.getScalingOffset(), 1e-5);
            assertEquals("kelvin", ch3bBand.getUnit());
            assertEquals(-54.529998779296875, ProductUtils.getGeophysicalSampleDouble(ch3bBand, 3, 3, 0), 1e-8);

            final Band ch4Band = product.getBand("ch4");
            assertNotNull(ch4Band);
            assertEquals(0.01, ch4Band.getScalingFactor(), 1e-8);
            assertEquals(273.15, ch4Band.getScalingOffset(), 1e-5);
            assertEquals("kelvin", ch4Band.getUnit());
            assertEquals(233.56999478116632, ProductUtils.getGeophysicalSampleDouble(ch4Band, 4, 4, 0), 1e-8);

            final Band cloudMaskBand = product.getBand("cloud_mask");
            assertNotNull(cloudMaskBand);
            assertEquals(7.0, ProductUtils.getGeophysicalSampleDouble(cloudMaskBand, 6, 6, 0), 1e-8);
            // todo 1 tb/tb continue with checking the indexcoding when the file format has been changed tb 2014-06-05
//            final IndexCoding indexCoding = cloudMaskBand.getIndexCoding();
//            assertNotNull(indexCoding);


            final Band cloudProbabilityBand = product.getBand("cloud_probability");
            assertNotNull(cloudProbabilityBand);
            assertEquals(128.0, ProductUtils.getGeophysicalSampleDouble(cloudProbabilityBand, 7, 7, 0), 1e-8);

            final Band dtimeBand = product.getBand("dtime");
            assertNotNull(dtimeBand);
            assertEquals("seconds", dtimeBand.getUnit());
            assertEquals(0.0, ProductUtils.getGeophysicalSampleDouble(dtimeBand, 8, 8, 0), 1e-8);

            final Band latBand = product.getBand("lat");
            assertNotNull(latBand);
            assertEquals("degrees_north", latBand.getUnit());
            assertEquals(48.35000228881836, ProductUtils.getGeophysicalSampleDouble(latBand, 9, 9, 0), 1e-8);

            final Band lonBand = product.getBand("lon");
            assertNotNull(lonBand);
            assertEquals("degrees_east", lonBand.getUnit());
            assertEquals(-173.11300659179688, ProductUtils.getGeophysicalSampleDouble(lonBand, 10, 10, 0), 1e-8);

            final Band qualFlagsBand = product.getBand("qual_flags");
            assertNotNull(qualFlagsBand);
            assertEquals(0.0, ProductUtils.getGeophysicalSampleDouble(qualFlagsBand, 11, 11, 0), 1e-8);
            final FlagCoding flagCoding = qualFlagsBand.getFlagCoding();
            //assertNotNull(flagCoding);
            // todo 1 tb/tb check for flag coding of this band tb 2014-06-04

            final Band relativeAzimuthAngleBand = product.getBand("relative_azimuth_angle");
            assertNotNull(relativeAzimuthAngleBand);
            assertEquals("angular_degree", relativeAzimuthAngleBand.getUnit());
            assertEquals(0.01, relativeAzimuthAngleBand.getScalingFactor(), 1e-8);
            assertEquals(-327.67999267578125, ProductUtils.getGeophysicalSampleDouble(relativeAzimuthAngleBand, 12, 12, 0), 1e-8);

            final Band satelliteZenithAngleBand = product.getBand("satellite_zenith_angle");
            assertNotNull(satelliteZenithAngleBand);
            assertEquals("angular_degree", satelliteZenithAngleBand.getUnit());
            assertEquals(0.01, satelliteZenithAngleBand.getScalingFactor(), 1e-8);
            assertEquals(61.869998617097735, ProductUtils.getGeophysicalSampleDouble(satelliteZenithAngleBand, 13, 13, 0), 1e-8);

            final Band solarZenithAngleBand = product.getBand("solar_zenith_angle");
            assertNotNull(solarZenithAngleBand);
            assertEquals("angular_degree", solarZenithAngleBand.getUnit());
            assertEquals(0.01, solarZenithAngleBand.getScalingFactor(), 1e-8);
            assertEquals(119.24999733455479, ProductUtils.getGeophysicalSampleDouble(solarZenithAngleBand, 14, 14, 0), 1e-8);
        } finally {
            product.dispose();
        }
    }
}
