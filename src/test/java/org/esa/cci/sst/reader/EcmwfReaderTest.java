package org.esa.cci.sst.reader;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class EcmwfReaderTest {

    @Test
    public void testOpenGribFile() throws IOException {
        assertNotNull(NetcdfFile.open("testdata/ECMWF/ggam201001310000.grb", null));
    }

    @Test
    public void testAcquireGribDataset() throws IOException {
        assertNotNull(NetcdfDataset.acquireDataset("testdata/ECMWF/ggam201001310000.grb", null));
    }

}
