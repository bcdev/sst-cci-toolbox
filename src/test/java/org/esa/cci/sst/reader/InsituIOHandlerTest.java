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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class InsituIOHandlerTest {

    @Test
    public void testReadObservation() throws Exception {
        final InsituIOHandler handler = createIOHandler();
        final InsituObservation observation;

        try {
            observation = handler.readObservation(0);
        } finally {
            handler.close();
        }

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.setTimeInMillis(observation.getTime().getTime());
        assertEquals(2007, calendar.get(Calendar.YEAR));
        assertEquals(11, calendar.get(Calendar.MONTH));
        assertEquals(17, calendar.get(Calendar.DATE));

        final double secondsPerDay = 86400.0;
        assertEquals(24.5, observation.getTimeRadius() / secondsPerDay, 0.0);

        final PGgeometry location = observation.getLocation();
        assertNotNull(location);

        final Geometry geometry = location.getGeometry();
        assertTrue(geometry instanceof LineString);

        final Point startPoint = geometry.getFirstPoint();
        assertEquals(88.92, startPoint.getX(), 0.0);
        assertEquals(9.750, startPoint.getY(), 0.0);

        final Point endPoint = geometry.getLastPoint();
        assertEquals(84.82, endPoint.getX(), 0.0);
        assertEquals(15.60, endPoint.getY(), 0.0);
    }

    @Test
    public void testFits() throws Exception {
        final double julianDateOf_2007_11_23_172624 = 2454428.185;

        Date testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_120000_000");
        assertTrue(InsituIOHandler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071124_020000_000");
        assertTrue(InsituIOHandler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_020000_000");
        assertFalse(InsituIOHandler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071124_060000_000");
        assertFalse(InsituIOHandler.fits(testTime, julianDateOf_2007_11_23_172624));

        final double julianDateOf_2008_01_11_222721 = 2454477.394;

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_220000_000");
        assertTrue(InsituIOHandler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_110000_000");
        assertTrue(InsituIOHandler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_080000_000");
        assertFalse(InsituIOHandler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20101124_050000_000");
        assertFalse(InsituIOHandler.fits(testTime, julianDateOf_2008_01_11_222721));
    }

    @Test
    public void testCreateFilledArray() throws Exception {
        Array fillArray = InsituIOHandler.createFillArray(DataType.INT, 15, new int[]{2, 3, 4});
        assertEquals(3, fillArray.getShape().length);
        assertEquals(24, fillArray.getSize());
        long size = fillArray.getSize();
        for (int i = 0; i < size; i++) {
            Object value = fillArray.getInt(i);
            assertEquals(15, value);
        }

        fillArray = InsituIOHandler.createFillArray(DataType.DOUBLE, Double.NEGATIVE_INFINITY, new int[]{5, 6, 7});
        assertEquals(3, fillArray.getShape().length);
        assertEquals(210, fillArray.getSize());
        size = fillArray.getSize();
        for (int i = 0; i < size; i++) {
            Object value = fillArray.getDouble(i);
            assertEquals(Double.NEGATIVE_INFINITY, value);
        }

    }

    private InsituIOHandler createIOHandler() throws Exception {
        final DataFile dataFile = new DataFile();
        final String path = getResourceAsFile("insitu_WMOID_11851_20071123_20080111.nc").getPath();
        dataFile.setPath(path);

        final InsituIOHandler handler = new InsituIOHandler();
        handler.init(dataFile);

        return handler;
    }

    private File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = getClass().getResource(name);
        final URI uri = url.toURI();

        return new File(uri);
    }
}
