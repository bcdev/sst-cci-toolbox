package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.SystemUtils;
import org.esa.cci.sst.util.BoundaryCalculator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.postgis.PGgeometry;

import javax.media.jai.JAI;
import java.io.File;

@Ignore
public class NcAvhrrGacProductReaderTest {

    @BeforeClass
    public static void init() throws Exception {
        SystemUtils.init3rdPartyLibs(NcAvhrrGacProductReaderTest.class.getClassLoader());
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(134217728);
    }

    @Test
    public void testReadTheFile() throws Exception {
        final File input = new File("/usr/local/data/avhrr-gac-test/19910101000000-AVHRR-L1b-AVHRR10_G-v02.0-fv01.0.nc");
        final NcAvhrrGacProductReader reader = new NcAvhrrGacProductReader(new NcAvhrrGacProductReaderPlugIn());

        final Product product = reader.readProductNodes(input, null);
        final BoundaryCalculator boundaryCalculator = new BoundaryCalculator();
        final PGgeometry geoBoundary = boundaryCalculator.getGeoBoundary(product);
        System.out.println("geoBoundary = " + geoBoundary);
    }
}
