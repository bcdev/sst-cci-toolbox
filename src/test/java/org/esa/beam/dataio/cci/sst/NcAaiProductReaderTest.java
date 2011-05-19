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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Storm
 */
public class NcAaiProductReaderTest {


    private AaiProductReaderPlugIn plugIn;
    private NcAaiProductReader reader;

    @Before
    public void setUp() throws Exception {
        plugIn = new AaiProductReaderPlugIn();
        reader = createMock();
    }

    @Test
    public void testCreatePlainProduct() throws Exception {
        final Product product = reader.createPlainProduct();
        String fileString = getClass().getResource("aai_20101224.nc").getFile();

        assertNotNull(product);
        assertEquals(new File(fileString).getAbsolutePath(), product.getFileLocation().getAbsolutePath());
        assertEquals("AerosolAai", product.getProductType());
        assertEquals("aai_20101224.nc", product.getName());
        assertEquals(288, product.getSceneRasterWidth());
        assertEquals(180, product.getSceneRasterHeight());
    }

    @Test
    public void testGetTimeString() throws Exception {
        final String timeString = reader.getTimeString();
        assertEquals("Tue May 17 10:32:00 2011", timeString);
    }

    @Test
    public void testAddMetadata() throws Exception {
        final NcAaiProductReader mock = mock(NcAaiProductReader.class);
        final String fileString = getClass().getResource("aai_20101224.nc").getFile();
        final NetcdfFile file = NetcdfFile.open(fileString);
        when(mock.getNetcdfFile()).thenReturn(file);
        doCallRealMethod().when(mock).addMetadata(Matchers.<Product>any());

        final Product product = new Product("dummy", "dummy", 10, 10);
        mock.addMetadata(product);

        final MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global Attributes");
        assertNotNull(globalAttributes);
        final MetadataAttribute[] attributes = globalAttributes.getAttributes();

        assertEquals(5, attributes.length);
        assertEquals("title", attributes[0].getName());
        assertEquals("Global Aerosol - Absorbing Aerosol Index", attributes[0].getData().getElemString());
        assertEquals("creation_date", attributes[3].getName());
        assertEquals("Tue May 17 10:32:00 2011", attributes[3].getData().getElemString());
    }

    private NcAaiProductReader createMock() {
        return new NcAaiProductReader(plugIn) {

            private String fileString;

            @Override
            protected NetcdfFile getNetcdfFile() {
                fileString = getClass().getResource("aai_20101224.nc").getFile();
                try {
                    return NetcdfFile.open(fileString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Object getInput() {
                return getClass().getResource("aai_20101224.nc").getFile();
            }
        };
    }
}
