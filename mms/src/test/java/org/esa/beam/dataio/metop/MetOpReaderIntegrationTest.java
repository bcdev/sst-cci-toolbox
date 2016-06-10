package org.esa.beam.dataio.metop;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Product;
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

    @Ignore
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

        } finally {
            product.dispose();
        }

    }
}
