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

import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class InsituReaderTest {

    @Test
    public void testReadObservation_SST_CCI_V1_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            final Calendar calendar = createUtcCalendar();
            calendar.setTimeInMillis(observation.getTime().getTime());
            assertEquals(2007, calendar.get(Calendar.YEAR));
            assertEquals(11, calendar.get(Calendar.MONTH));
            assertEquals(18, calendar.get(Calendar.DATE));

            assertEquals(2125828.8, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertTrue(geometry instanceof LineString);

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(88.92, startPoint.getX(), 1e-8);
            assertEquals(9.750, startPoint.getY(), 1e-8);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(84.82, endPoint.getX(), 1e-8);
            assertEquals(15.60, endPoint.getY(), 1e-8);
        }
    }

    @Test
    public void testRead_SST_CCI_V1_Data() throws Exception {
        final Calendar calendar = creatUtcCalendar(2007, 11, 18);

        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder();
        final ReferenceObservation refObs = new ReferenceObservation();
        refObs.setTime(calendar.getTime());
        builder.referenceObservation(refObs);
        builder.shape(new int[]{1, 14});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            Array array = reader.read("sst", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(300.31, array.getDouble(0), 1e-5);
            assertEquals(300.31, array.getDouble(7), 1e-5);
            assertEquals(300.22998046875, array.getDouble(13), 1e-5);

            array = reader.read("lon", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(85.5140076, array.getDouble(0), 1e-5);
            assertEquals(85.4431534, array.getDouble(7), 1e-5);
            assertEquals(85.3783264, array.getDouble(13), 1e-5);

            array = reader.read("lat", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(14.2022161, array.getDouble(0), 1e-5);
            assertEquals(14.1622972, array.getDouble(7), 1e-5);
            assertEquals(14.107686, array.getDouble(13), 1e-5);

            array = reader.read("time", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454452.519), array.getDouble(0), 1e-3);
            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454453.060), array.getDouble(7), 1e-3);
            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454453.477), array.getDouble(13), 1e-3);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V1_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(1285, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(9.753368, 88.9182129, 1195835184000L, 11851, samplingPoint);

            samplingPoint = samplingPoints.get(619);
            assertCorrectSamplingPoint(14.048774, 85.6864777, 1197883641600L, 11851, samplingPoint);

            samplingPoint = samplingPoints.get(1284);
            assertCorrectSamplingPoint(15.607534, 84.8221817, 1200086841600L, 11851, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Drifter_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_0_WMOID_71569_20030117_20030131.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            final Calendar calendar = createUtcCalendar();
            calendar.setTimeInMillis(observation.getTime().getTime());
            assertEquals(2003, calendar.get(Calendar.YEAR));
            assertEquals(0, calendar.get(Calendar.MONTH));
            assertEquals(24, calendar.get(Calendar.DATE));

            assertEquals(636173.5, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertTrue(geometry instanceof LineString);

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(-56.04999923706055, startPoint.getX(), 1e-8);
            assertEquals(-60.0, startPoint.getY(), 1e-8);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(-56.77000045776367, endPoint.getX(), 1e-8);
            assertEquals(-60.77000045776367, endPoint.getY(), 1e-8);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Drifter_Data() throws Exception {
        final Calendar calendar = creatUtcCalendar(2003, 0, 25);

        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder();
        final ReferenceObservation refObs = new ReferenceObservation();
        refObs.setTime(calendar.getTime());
        builder.referenceObservation(refObs);
        builder.shape(new int[]{1, 12});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_0_WMOID_71569_20030117_20030131.nc")) {
            Array array = reader.read("sst", extractDefinition);
            assertNotNull(array);
            assertEquals(12, array.getSize());

            assertEquals(2.2, array.getDouble(0), 1e-6);
            assertEquals(2.2, array.getDouble(5), 1e-6);
            assertEquals(2.3, array.getDouble(11), 1e-6);

            array = reader.read("lon", extractDefinition);
            assertNotNull(array);
            assertEquals(12, array.getSize());

            assertEquals(-59.009998, array.getDouble(0), 1e-6);
            assertEquals(-58.860001, array.getDouble(5), 1e-6);
            assertEquals(-58.540001, array.getDouble(11), 1e-6);

            array = reader.read("lat", extractDefinition);
            assertNotNull(array);
            assertEquals(12, array.getSize());

            assertEquals(-60.369999, array.getDouble(0), 1e-6);
            assertEquals(-60.450001, array.getDouble(5), 1e-6);
            assertEquals(-60.540001, array.getDouble(11), 1e-6);

            array = reader.read("time", extractDefinition);
            assertNotNull(array);
            assertEquals(12, array.getSize());

            assertEquals(790992179, array.getDouble(0), 1e-6);
            assertEquals(791033112, array.getDouble(5), 1e-6);
            assertEquals(791077212, array.getDouble(11), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Drifter_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_0_WMOID_71569_20030117_20030131.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(223, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(-60.0, -56.05, 1042784460000L, 71569, samplingPoint);

            samplingPoint = samplingPoints.get(107);
            assertCorrectSamplingPoint(-60.27, -59.14, 1043403372000L, 71569, samplingPoint);

            samplingPoint = samplingPoints.get(222);
            assertCorrectSamplingPoint(-60.77, -56.77, 1044056807000L, 71569, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Argo_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_5_WMOID_7900016_20030110_20030130.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            final Calendar calendar = createUtcCalendar();
            calendar.setTimeInMillis(observation.getTime().getTime());
            assertEquals(2003, calendar.get(Calendar.YEAR));
            assertEquals(0, calendar.get(Calendar.MONTH));
            assertEquals(20, calendar.get(Calendar.DATE));

            assertEquals(859463.0, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertTrue(geometry instanceof LineString);

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(4.541999816894531, startPoint.getX(), 1e-8);
            assertEquals(-62.78499984741211, startPoint.getY(), 1e-8);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(3.2060000896453857, endPoint.getX(), 1e-8);
            assertEquals(-62.749000549316406, endPoint.getY(), 1e-8);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Argo_Data() throws Exception {
        final Calendar calendar = creatUtcCalendar(2003, 0, 20);

        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder();
        final ReferenceObservation refObs = new ReferenceObservation();
        refObs.setTime(calendar.getTime());
        builder.referenceObservation(refObs);
        builder.shape(new int[]{1, 3});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_5_WMOID_7900016_20030110_20030130.nc")) {
            Array array = reader.read("sst", extractDefinition);
            assertNotNull(array);
            assertEquals(3, array.getSize());

            assertEquals(0.621, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
            assertEquals(-32768.0, array.getDouble(2), 1e-6);

            array = reader.read("lon", extractDefinition);
            assertNotNull(array);
            assertEquals(3, array.getSize());

            assertEquals(3.8989999, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
            assertEquals(-32768.0, array.getDouble(2), 1e-6);

            array = reader.read("lat", extractDefinition);
            assertNotNull(array);
            assertEquals(3, array.getSize());

            assertEquals(-62.625, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
            assertEquals(-32768.0, array.getDouble(2), 1e-6);

            array = reader.read("time", extractDefinition);
            assertNotNull(array);
            assertEquals(3, array.getSize());

            assertEquals(790590428, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
            assertEquals(-32768.0, array.getDouble(2), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Argo_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_5_WMOID_7900016_20030110_20030130.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(3, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(-62.785, 4.542, 1042188973000L, 7900016, samplingPoint);

            samplingPoint = samplingPoints.get(1);
            assertCorrectSamplingPoint(-62.625, 3.899, 1043051228000L, 7900016, samplingPoint);

            samplingPoint = samplingPoints.get(2);
            assertCorrectSamplingPoint(-62.749, 3.206, 1043907899000L, 7900016, samplingPoint);
        }
    }

    @Test
    public void testCreateSubset_1D() throws InvalidRangeException {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final Range range = InsituReaderHelper.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, range, 10);
        final Array subset = Array.factory(historyTimes.getElementType(), new int[]{1, 10});
        InsituReader.extractSubset(historyTimes, subset, s);

        assertEquals(2, subset.getRank());
        assertEquals(10, subset.getIndexPrivate().getShape(1));
        assertEquals(historyTimes.getDouble(s.get(0).first()), subset.getDouble(0), 0.0);
        assertEquals(historyTimes.getDouble(s.get(9).first()), subset.getDouble(9), 0.0);
    }

    @Test
    public void testCreateSubset_2D() throws InvalidRangeException {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final int historyLength = historyTimes.getIndexPrivate().getShape(0);
        final Range range = InsituReaderHelper.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, range, 10);

        final Array array = Array.factory(DataType.INT, new int[]{historyLength, 2});
        final Array subset = Array.factory(array.getElementType(), new int[]{1, 10, 2});
        InsituReader.extractSubset(array, subset, s);

        assertEquals(3, subset.getRank());
        assertEquals(10, subset.getIndexPrivate().getShape(1));
        assertEquals(2, subset.getIndexPrivate().getShape(2));
    }

    @Test
    public void testNormalizeLon() {
        assertEquals(26.0, InsituReader.normalizeLon(26.0), 1e-8);
        assertEquals(-107.0, InsituReader.normalizeLon(-107.0), 1e-8);

        assertEquals(-0.1, InsituReader.normalizeLon(359.9), 1e-8);
        assertEquals(0.1, InsituReader.normalizeLon(-359.9), 1e-8);

        assertEquals(-19.0, InsituReader.normalizeLon(-379.0), 1e-8);
        assertEquals(3.0, InsituReader.normalizeLon(363.0), 1e-8);
    }

    @Test
    public void testGetNumRecords() {
        final InsituReader reader = new InsituReader("whatever");

        assertEquals(1, reader.getNumRecords());
    }

    @Test
    public void testGetGeoCoding() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        assertNull(reader.getGeoCoding(34));
    }

    @Test
    public void testGetDTime() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getDTime(23, 45);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetTime() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getTime(23, 45);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetInsituSource() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        assertNull(reader.getInsituSource());
    }

    @Test
    public void testGetScanLineCount() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getScanLineCount();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetElement() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getElementCount();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testIsNotOnPlanet() {
        assertFalse(InsituReader.isNotOnPlanet(0.0, 0.0));
        assertFalse(InsituReader.isNotOnPlanet(-17.0, 56.0));
        assertFalse(InsituReader.isNotOnPlanet(38.0, -36.0));

        assertTrue(InsituReader.isNotOnPlanet(90.1, -36.0));
        assertTrue(InsituReader.isNotOnPlanet(-90.1, -36.0));
        assertTrue(InsituReader.isNotOnPlanet(18.3, -180.1));
        assertTrue(InsituReader.isNotOnPlanet(18.3, 180.1));
    }

    private static InsituReader createReader(String resourceName) throws Exception {
        final DataFile dataFile = new DataFile();
        final String path = getResourceAsFile(resourceName).getPath();
        dataFile.setPath(path);

        final InsituReader reader = new InsituReader("history");
        reader.init(dataFile, null);

        return reader;
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = InsituReaderTest.class.getResource(name);
        final URI uri = url.toURI();

        return new File(uri);
    }

    private static Calendar createUtcCalendar() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
    }

    private static Calendar creatUtcCalendar(int year, int month, int day) {
        final Calendar calendar = createUtcCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

    private static void assertCorrectSamplingPoint(double lat, double lon, long time, int reference, SamplingPoint samplingPoint) {
        assertEquals(lat, samplingPoint.getLat(), 1e-6);
        assertEquals(lon, samplingPoint.getLon(), 1e-6);
        assertEquals(time, samplingPoint.getTime());
        assertEquals(reference, samplingPoint.getReference());
    }
}
