/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class NcAaiProductReaderTest {


    private NcAaiProductReader reader;
    private Product product;

    @Before
    public void setUp() throws IOException {
        reader = new NcAaiProductReader(new NcAaiProductReaderPlugIn()) {

            @Override
            protected NetcdfFile getNetcdfFile() {
                final String location = getInput();
                try {
                    return NetcdfFile.open(location);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public String getInput() {
                return getClass().getResource("aai_20101224.nc").getFile();
            }
        };
        product = reader.createPlainProduct();
    }

    @Test
    public void testPlainProduct() throws Exception {
        assertNotNull(product);
        assertEquals("aai_20101224.nc", product.getName());
        assertEquals("AerosolAai", product.getProductType());
        assertEquals(288, product.getSceneRasterWidth());
        assertEquals(180, product.getSceneRasterHeight());
    }

    @Test
    public void testSetTime() throws Exception {
        reader.setTime(product);

        final ProductData.UTC productStartTime = product.getStartTime();
        final ProductData.UTC expectedStartTime = ProductData.UTC.parse("2010-12-24-00-00", "yyyy-MM-dd-HH-mm");

        assertNotNull(productStartTime);
        assertEquals(expectedStartTime.getMJD(), productStartTime.getMJD(), 0.0);

        final ProductData.UTC productEndTime = product.getEndTime();
        final ProductData.UTC expectedEndTime = ProductData.UTC.parse("2010-12-25-00-00", "yyyy-MM-dd-HH-mm");

        assertNotNull(productEndTime);
        assertEquals(expectedEndTime.getMJD(), productEndTime.getMJD(), 0.0);
    }

    @Test
    public void testAddMetadata() throws Exception {
        reader.addMetadata(product);

        final MetadataElement globalAttributes = product.getMetadataRoot().getElementAt(0);
        assertNotNull(globalAttributes);
        final MetadataAttribute[] attributes = globalAttributes.getAttributes();

        assertEquals(5, attributes.length);
        assertEquals("title", attributes[0].getName());
        assertEquals("Global Aerosol - Absorbing Aerosol Index", attributes[0].getData().getElemString());
        assertEquals("creation_date", attributes[3].getName());
        assertEquals("Tue May 17 10:32:00 2011", attributes[3].getData().getElemString());
    }

    @Test
    public void testGetMinAndMaxValue() throws Exception {
        final String testFile = getClass().getResource("aai_20101224.nc").getFile();
        final NetcdfFile file = NetcdfFile.open(testFile);
        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;
        final Variable variable = file.findVariable(NetcdfFile.escapeName("aerosol_absorbing_index"));
        final Array array = variable.read();
        final IndexIterator iterator = array.getIndexIterator();
        while(iterator.hasNext()) {
            final float currentValue = iterator.getFloatNext();
            if(currentValue == (Float) variable.findAttribute("_FillValue").getValue(0)) {
                continue;
            }
            if(currentValue < minValue) {
                minValue = currentValue;
            }
            if(currentValue > maxValue) {
                maxValue = currentValue;
            }
        }

        System.out.println(MessageFormat.format("minValue = {0}", minValue));
        System.out.println(MessageFormat.format("maxValue = {0}", maxValue));
    }
}
