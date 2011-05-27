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
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class EgrAaiProductReaderTest {

    private static final int COL_COUNT = 288;
    private static final int ROW_COUNT = 180;
    private static final String RESOURCE_NAME = "20100601.egr";

    @Test
    public void testReferenceTime() throws URISyntaxException, IOException {
        final ProductData.UTC time = EgrAaiProductReader.readReferenceTime(getResourceAsFile(RESOURCE_NAME));
        assertEquals(TimeZone.getTimeZone("UTC"), time.getAsCalendar().getTimeZone());
        assertEquals(2010, time.getAsCalendar().get(Calendar.YEAR));
        assertEquals(5, time.getAsCalendar().get(Calendar.MONTH));
        assertEquals(1, time.getAsCalendar().get(Calendar.DATE));
        assertEquals(0, time.getAsCalendar().get(Calendar.HOUR_OF_DAY));
        assertEquals(0, time.getAsCalendar().get(Calendar.MINUTE));
        assertEquals(0, time.getAsCalendar().get(Calendar.SECOND));
        assertEquals(0, time.getAsCalendar().get(Calendar.MILLISECOND));
    }

    @Test
    public void testGeoCoding() {
        final GeoCoding geoCoding = EgrAaiProductReader.createGeoCoding(new Dimension(COL_COUNT, ROW_COUNT));
        assertNotNull(geoCoding);
        assertTrue(geoCoding.canGetGeoPos());
        assertTrue(geoCoding.canGetPixelPos());
        assertSame(DefaultGeographicCRS.WGS84, geoCoding.getMapCRS());

        final PixelPos upperLeft = new PixelPos(0.5f, 0.5f);
        final GeoPos northWest = geoCoding.getGeoPos(upperLeft, new GeoPos());
        assertEquals(-89.5, northWest.getLat(), 0.0);
        assertEquals(-179.375, northWest.getLon(), 0.0);

        final PixelPos lowerRight = new PixelPos(COL_COUNT - 0.5f, ROW_COUNT - 0.5f);
        final GeoPos southEast = geoCoding.getGeoPos(lowerRight, new GeoPos());
        assertEquals(89.5, southEast.getLat(), 0.0);
        assertEquals(179.375, southEast.getLon(), 0.0);
    }

    @Test
    public void testSourceImage() throws URISyntaxException {
        final EgrAaiProductReader.SampleReader sampleReader =
                EgrAaiProductReader.createSampleReader(getResourceAsFile(RESOURCE_NAME));
        assertNotNull(sampleReader);
        final RenderedImage sourceImage =
                EgrAaiProductReader.createSourceImage(ProductData.TYPE_UINT16, COL_COUNT, ROW_COUNT, sampleReader);
        assertNotNull(sourceImage);
        assertEquals(COL_COUNT, sourceImage.getWidth());
        assertEquals(ROW_COUNT, sourceImage.getHeight());
        assertEquals(1, sourceImage.getNumXTiles());
        assertEquals(1, sourceImage.getNumYTiles());
        assertEquals(DataBuffer.TYPE_USHORT, sourceImage.getSampleModel().getDataType());

        final Raster samples = sourceImage.getData();
        assertEquals(999, getSample(samples, 0, ROW_COUNT - 1));
        assertEquals(443, getSample(samples, 2, ROW_COUNT - 3));
        assertEquals(440, getSample(samples, COL_COUNT - 3, ROW_COUNT - 3));
        assertEquals(439, getSample(samples, 0, ROW_COUNT - 4));
        assertEquals(441, getSample(samples, COL_COUNT - 1, ROW_COUNT - 4));
        assertEquals(999, getSample(samples, COL_COUNT - 1, 0));
    }

    @Test
    public void testReadSamples() throws URISyntaxException {
        final Number[] samples = EgrAaiProductReader.readSamples(getResourceAsFile(RESOURCE_NAME));
        assertEquals(COL_COUNT * ROW_COUNT, samples.length);
        assertEquals(999, getSample(samples, 0, 0));
        assertEquals(443, getSample(samples, 2, ROW_COUNT - 3));
        assertEquals(440, getSample(samples, COL_COUNT - 3, ROW_COUNT - 3));
        assertEquals(439, getSample(samples, 0, ROW_COUNT - 4));
        assertEquals(441, getSample(samples, COL_COUNT - 1, ROW_COUNT - 4));
        assertEquals(999, getSample(samples, COL_COUNT - 1, 0));
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = EgrAaiProductReaderTest.class.getResource(name);
        final URI uri = url.toURI();

        return new File(uri);
    }

    private static short getSample(Number[] samples, int x, int y) {
        return samples[y * COL_COUNT + x].shortValue();
    }

    private static int getSample(Raster samples, int x, int y) {
        return samples.getSample(x, y, 0);
    }
}
