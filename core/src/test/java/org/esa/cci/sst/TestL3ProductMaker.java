/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
        return getNetcdfFile("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.nc");
    }

    public static NetcdfFile readL4GridsSetup() throws IOException, URISyntaxException {
        return getNetcdfFile("20100701000000-ESACCI-L4_GHRSST-SSTdepth-OSTIA-LT-v02.0-fv01.1.nc");
    }

    private static NetcdfFile getNetcdfFile(String l3) throws URISyntaxException, IOException {
        final URL url = TestL3ProductMaker.class.getResource(l3);
        final URI uri = url.toURI();
        final File file = new File(uri);

        return NetcdfFile.open(file.getPath());
    }
}
