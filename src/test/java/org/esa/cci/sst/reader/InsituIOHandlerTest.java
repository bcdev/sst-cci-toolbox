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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
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
