package org.esa.beam.dataio.metop;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.cci.sst.IoTestRunner;
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
    public void testDUMMY() throws IOException {
        // @todo 1 tb/tb make this a general resource access pattern 2016-06-10
        final File file = new File("/fs1/projects/ongoing/SST-CCI/data/testData/AVHR_xxx_1B_M02_20080211161603Z_20080211175803Z_N_O_20080211175632Z.nat");
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

            final TiePointGrid longitude = product.getTiePointGrid("longitude");
            assertNotNull(longitude);
            assertEquals(-123.30519104003906, longitude.getPixelDouble(10, 10), 1e-8);

            final TiePointGrid latitude = product.getTiePointGrid("latitude");
            assertNotNull(latitude);
            assertEquals(79.19400024414062, latitude.getPixelDouble(20, 20), 1e-8);

            final Band reflectance_1 = product.getBand("reflec_1");
            assertNotNull(reflectance_1);
            final double[] doubles = new double[1];
            reflectance_1.readPixels(30, 30, 1, 1, doubles);
            assertEquals(0.0019087587716057897, doubles[0] , 1e-8);
        } finally {
            product.dispose();
        }

    }
}
