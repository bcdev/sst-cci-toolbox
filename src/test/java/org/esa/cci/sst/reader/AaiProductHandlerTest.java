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

package org.esa.cci.sst.reader;

import org.esa.beam.dataio.cci.sst.AaiProductReaderTest;
import org.esa.cci.sst.data.ColumnI;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Observation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

public class AaiProductHandlerTest {

    private static final String AAI_RESOURCE_NAME = "20100601.egr";

    private static DataFile dataFile;
    private static AbstractProductHandler handler;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        final URL url = AaiProductReaderTest.class.getResource(AAI_RESOURCE_NAME);
        final URI uri = url.toURI();
        final File file = new File(uri);

        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        handler = new AaiProductHandler("aai");
        handler.init(dataFile);
    }

    @AfterClass
    public static void clean() {
        if (handler != null) {
            handler.close();
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
        final Observation observation = handler.readObservation(0);

        assertTrue(observation instanceof GlobalObservation);
        assertSame(dataFile, observation.getDatafile());
    }

    @Test
    public void testGetColumns() throws URISyntaxException, IOException {
        final ColumnI[] columns = handler.getColumns();

        assertEquals(1, columns.length);
    }
}
