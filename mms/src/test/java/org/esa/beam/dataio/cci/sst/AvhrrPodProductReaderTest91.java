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

import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
@Ignore
public class AvhrrPodProductReaderTest91 {

    private AvhrrPodProductReader reader;
    private Product product;

    @Before
    public void setUp() throws IOException {
        final AvhrrPodProductReaderPlugIn plugIn = new AvhrrPodProductReaderPlugIn();
        reader = (AvhrrPodProductReader) plugIn.createReaderInstance();
        final String fileName = getClass().getResource("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC").getFile();
        product = reader.readProductNodes(fileName, null);
    }

    @Test
    public void testCreateProduct() throws Exception {
        assertEquals("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC", product.getName());
        assertEquals("AVHRR GAC POD", product.getProductType());
        assertEquals(409, product.getSceneRasterWidth());
        assertEquals(12110, product.getSceneRasterHeight());
        assertSame(reader, product.getProductReader());
        assertEquals("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC", product.getFileLocation().getName());
        assertEquals(2, product.getTiePointGrids().length);

    }

    @Test
    public void testGetStartTime() throws Exception {
        final Date startTime = reader.getStartTime().getAsDate();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh mm");
        final String formattedStartTime = sdf.format(startTime);
        assertEquals("1991 09 03 12 13", formattedStartTime);
    }

    @Test
    public void testGetEndTime() throws Exception {
        final Date endTime = reader.getEndTime().getAsDate();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh mm");
        final String formattedEndTime = sdf.format(endTime);
        assertEquals("1991 09 03 01 54", formattedEndTime);
    }

    @Test
    public void testGetNumRecords() throws Exception {
        assertEquals(12111, reader.getNumRecords());
    }

}
