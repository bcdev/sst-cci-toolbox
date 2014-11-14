package org.esa.cci.sst;

import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Bettina Scholze
 *         Date: 26.07.12 10:30
 */
public class TestL3ProductMaker {

    public static NetcdfFile readL3GridsSetup() throws IOException, URISyntaxException {
        final URL url = TestL3ProductMaker.class.getResource(
                "20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
        final URI uri = url.toURI();
        final File file = new File(uri);

        return NetcdfFile.open(file.getPath());
    }

    public static NetcdfFile readL4GridsSetup() throws IOException, URISyntaxException {
        final URL url = TestL3ProductMaker.class.getResource(
                "20100701000000-ESACCI-L4_GHRSST-SSTdepth-OSTIA-LT-v02.0-fv01.1.nc");
        final URI uri = url.toURI();
        final File file = new File(uri);

        return NetcdfFile.open(file.getPath());
    }
}
