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

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
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

    private static final double HALF_DAY_MJD = 0.5;
    private static final int HALF_DAY_SECS = 43200;

    @Test
    public void testReadSST_CCI_V1_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader handler = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            observation = handler.readObservation(0);

            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
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

    // @todo 1 tb/tb continue here 2014-01-29
//    @Test
//    public void testReadSST_CCI_V2_Argo_Data() throws Exception {
//        final InsituObservation observation;
//
//        try (InsituReader handler = createReader("SSTCCI2_refdata_200002_argo_sample.nc")) {
//
//        }
//    }

    @Test
    public void testFindRange_illegalRank() {
        final double[][] twoDims = {{1.0, 2.0}, {3.0, 4.0}};
        final Array historyTimes = Array.factory(twoDims);

        try {
            InsituReader.findRange(historyTimes, 12, HALF_DAY_MJD);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testFindRange_ForReferenceTimeInHistory_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2455090.56;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeInHistory_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790904339;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtStartOfHistory_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first()) == referenceTime);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtStartOfHistory_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first()) == referenceTime);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtEndOfHistory_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last()) == referenceTime);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtEndOfHistory_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 791596007;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_SECS);
        assertEquals(referenceTime, historyTimes.getDouble(range.last()), 1e-8);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtLowerLimit_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446 - HALF_DAY_MJD;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtLowerLimit_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660 - HALF_DAY_SECS;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtUpperLimit_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774 + HALF_DAY_MJD;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime - HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtUpperLimit_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 791596007 + HALF_DAY_SECS;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime - HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeBeforeHistory_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446 - 2 * HALF_DAY_MJD;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeBeforeHistory_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660 - 2 * HALF_DAY_SECS;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeAfterHistory_MJD() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774 + 2 * HALF_DAY_MJD;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeAfterHistory_SECS_1978() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final double referenceTime = 791596007 + 2 * HALF_DAY_SECS;
        final Range range = InsituReader.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testCreateSubsampling() {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final Range r = InsituReader.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReader.createSubsampling(historyTimes, r, 10);

        assertEquals(10, s.size());
        assertEquals(r.first(), s.get(0).first());
        assertEquals(r.first(), s.get(0).last());
        assertEquals(r.last(), s.get(9).first());
        assertEquals(r.last(), s.get(9).last());
    }

    @Test
    public void testCreateSubset_1D() throws InvalidRangeException {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final Range range = InsituReader.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReader.createSubsampling(historyTimes, range, 10);
        final Array subset = Array.factory(historyTimes.getElementType(), new int[]{1, 10});
        InsituReader.extractSubset(historyTimes, subset, s);

        assertEquals(2, subset.getRank());
        assertEquals(10, subset.getIndexPrivate().getShape(1));
        assertEquals(historyTimes.getDouble(s.get(0).first()), subset.getDouble(0), 0.0);
        assertEquals(historyTimes.getDouble(s.get(9).first()), subset.getDouble(9), 0.0);
    }

    @Test
    public void testCreateSubset_2D() throws InvalidRangeException {
        final Array historyTimes = createHistoryTimeArray_MJD();
        final int historyLength = historyTimes.getIndexPrivate().getShape(0);
        final Range range = InsituReader.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReader.createSubsampling(historyTimes, range, 10);

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

    private static Array createHistoryTimeArray_MJD() {
        return Array.factory(InsituData.MJD_TIMES);
    }

    private static Array createHistoryTimeArray_SECS_1978() {
        return Array.factory(InsituData.SECS_1978_TIMES_DRIFTER);
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
}
