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

import org.esa.beam.dataio.cci.sst.EgrAaiProductReaderTest;
import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.util.SamplingPoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class AaiProductReaderTest {

    private static final String AAI_RESOURCE_NAME = "20100601.egr";

    private static DataFile dataFile;
    private static AbstractProductReader reader;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        final String filePath = TestHelper.getResourcePath(EgrAaiProductReaderTest.class, AAI_RESOURCE_NAME);
        final File file = new File(filePath);
        assertTrue(file.isFile());

        reader = new AaiProductReader("aai");
        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        final Sensor sensor = new SensorBuilder().
                name(reader.getSensorName()).
                observationType(GlobalObservation.class).
                build();
        dataFile.setSensor(sensor);
        reader.init(dataFile, null);
    }

    @AfterClass
    public static void clean() {
        if (reader != null) {
            reader.close();
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        assertEquals(1, reader.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
        final Observation observation = reader.readObservation(0);

        assertTrue(observation instanceof GlobalObservation);
        assertSame(dataFile, observation.getDatafile());
    }

    @Test
    public void testGetColumns() throws URISyntaxException, IOException {
        final Item[] columns = reader.getColumns();

        assertEquals(1, columns.length);
    }

    @Test
    public void testGetSamplingPoints() {
        final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
        assertNotNull(samplingPoints);
        assertEquals(0, samplingPoints.size());
    }
}
