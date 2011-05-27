/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class HdfOsiProductReaderTest {

    private HdfOsiProductReader reader;

    @Test
    public void testReadProductNodesImpl() throws Exception {
        final File inputFile = getIceConcentrationFile();
        final Product product = reader.readProductNodes(inputFile, null);

        assertNotNull(product);
        assertTrue(product.getProductReader() == reader);
        assertEquals(inputFile, product.getFileLocation());
        assertEquals(1, product.getNumBands());
        assertNotNull(product.getGeoCoding());
        assertNotNull(product.getBandAt(0).getSourceImage());
        assertNotNull(product.getStartTime());
        assertNotNull(product.getEndTime());

        final ProductData.UTC expectedStartTime = ProductData.UTC.parse("2010-06-30-00-00", "yyyy-MM-dd-HH-mm");

        assertEquals(expectedStartTime.getMJD(), product.getStartTime().getMJD(), 0.0);

        final ProductData.UTC expectedEndTime = ProductData.UTC.parse("2010-07-01-00-00", "yyyy-MM-dd-HH-mm");

        assertEquals(expectedEndTime.getMJD(), product.getEndTime().getMJD(), 0.0);

    }

    @Test
    public void testSetTime() throws Exception {
        final Product product = new Product("P", "T", 10, 10);
        reader.setTime(product, 2011, 2, 16, 12, 0);

        final ProductData.UTC productStartTime = product.getStartTime();
        final ProductData.UTC expectedStartTime = ProductData.UTC.parse("2011-02-16-00-00", "yyyy-MM-dd-HH-mm");

        assertNotNull(productStartTime);
        assertEquals(expectedStartTime.getMJD(), productStartTime.getMJD(), 0.0);

        final ProductData.UTC productEndTime = product.getEndTime();
        final ProductData.UTC expectedEndTime = ProductData.UTC.parse("2011-02-17-00-00", "yyyy-MM-dd-HH-mm");

        assertNotNull(productEndTime);
        assertEquals(expectedEndTime.getMJD(), productEndTime.getMJD(), 0.0);
    }

    @Test
    public void testGetMetadata() throws Exception {
        final NetcdfFile ncFile = NetcdfFile.open(getIceConcentrationFile().getPath());
        final Variable header = ncFile.findVariable("Header");
        final MetadataElement metadata = reader.getMetadata((Structure) header);
        assertNotNull(metadata);
        assertTrue(metadata.getAttributes() != null && metadata.getAttributes().length > 0);
        assertEquals(760, metadata.getAttribute("Header.iw").getData().getElemInt());
        assertEquals(1120, metadata.getAttribute("Header.ih").getData().getElemInt());
        assertEquals(-3850.0, metadata.getAttribute("Header.Bx").getData().getElemFloat(), 0.0);
        assertEquals(5850, metadata.getAttribute("Header.By").getData().getElemFloat(), 0.0);
        assertEquals(HdfOsiProductReader.NORTHERN_HEMISPHERE, metadata.getAttribute("Header.area").getData().getElemString());
    }

    @Test
    public void testIsSeaIceFile() throws Exception {
        assertTrue(HdfOsiProductReader.isSeaIceFile(getIceConcentrationFile()));
        assertFalse(HdfOsiProductReader.isSeaIceFile(getIceConcentrationQualityFile()));
    }

    @Test
    public void testGeoCoding() throws Exception {
        final NetcdfFile ncFile = NetcdfFile.open(getIceConcentrationFile().getPath());
        final Variable header = ncFile.findVariable("Header");
        final GeoCoding geoCoding = reader.createGeoCoding((Structure) header);
        assertNotNull(geoCoding);
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        pixelPos.setLocation(688.18774, 943.7666);
        geoCoding.getGeoPos(pixelPos, geoPos);
        assertEquals(48.483643, geoPos.lat, 0.01);
        assertEquals(-4.7993994, geoPos.lon, 0.01);

        pixelPos.setLocation(718.5, 820.5);
        geoCoding.getGeoPos(pixelPos, geoPos);
        assertEquals(53.59092, geoPos.lat, 0.5);
        assertEquals(9.785294, geoPos.lon, 0.5);
    }

    @Before
    public void setUp() throws Exception {
        reader = new HdfOsiProductReader(new HdfOsiProductReaderPlugIn());
    }

    @After
    public void closeReader() throws IOException {
        reader.close();
    }

    private static File getIceConcentrationFile() throws URISyntaxException {
        return getResourceAsFile("ice_conc_nh_201006301200.hdf");
    }

    private File getIceConcentrationQualityFile() throws URISyntaxException {
        return getResourceAsFile("ice_conc_sh_qual_201006301200.hdf");
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        return new File(HdfOsiProductReaderPlugInTest.class.getResource(name).toURI());
    }

    public static void main() throws IOException, URISyntaxException {
        final File file = getIceConcentrationFile();
        assertTrue(NetcdfFile.canOpen(file.getPath()));

        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFile.open(file.getPath());

            final List<Variable> variableList = netcdfFile.getVariables();
            for (Variable v : variableList) {
                System.out.println("v.getName() = " + v.getName());
            }

            final Structure header = (Structure) netcdfFile.findVariable("Header");
            for (final Variable v : header.getVariables()) {
                System.out.println("v.getName() = " + v.getName());
                switch (v.getDataType()) {
                    case CHAR:
                        System.out.println("v.value = " + v.readScalarString());
                        break;
                    case FLOAT:
                        System.out.println("v.value = " + v.readScalarFloat());
                        break;
                    case INT:
                        System.out.println("v.value = " + v.readScalarInt());
                        break;
                    case SHORT:
                        System.out.println("v.value = " + v.readScalarShort());
                        break;
                }
            }
            final List<Attribute> attributeList = header.getAttributes();
            for (Attribute a : attributeList) {
                System.out.println("a.getName() = " + a.getName());
                System.out.println("a.getValue() = " + a.getValue(0));
            }
            final Variable data = netcdfFile.findVariable("Data/data[00]");
            System.out.println("data.getName() = " + data.getName());

            System.out.println(netcdfFile.toString());
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

}
