package org.esa.cci.sst.reader;

import org.junit.Test;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.junit.Assert.*;

public class EcmwfReaderTest {

    @Test
    public void testOpenGribFile() throws IOException {
        assertNotNull(NetcdfFile.open("testdata/ECMWF/ggam201001310000.grb", null));
        NCdumpW.printHeader("testdata/ECMWF/ggam201001310000.grb", new OutputStreamWriter(System.out));
    }

    @Test
    public void testAcquireGribDataset() throws IOException {
        assertNotNull(NetcdfDataset.acquireDataset("testdata/ECMWF/ggam201001310000.grb", null));
    }

    String fileLocation = "testdata\\ECMWF\\gafs201001310003.nc";

    @Test
    public void testReadHeader() throws Exception {
        assertTrue(NetcdfFile.canOpen(fileLocation));
        final NetcdfFile ncFile = NetcdfFile.open(fileLocation);
        NCdumpW.printHeader(fileLocation, new OutputStreamWriter(System.out));
    }    
    
}
