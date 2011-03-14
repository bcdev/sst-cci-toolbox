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

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class InsituHistoryIOHandlerTest {

    private InsituHistoryIOHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new InsituHistoryIOHandler();
    }

    @Test
    public void testGetTime() throws Exception {
        InsituHistoryIOHandler.TimeInterval time = handler.getTime("insitu_WMOID_11851_20100101_20100101.nc");
        Date test = new GregorianCalendar(2010, 0, 1).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());
        assertEquals(0, time.timeRadius);

        time = handler.getTime("insitu_WMOID_11851_20100101_20100103.nc");
        test = new GregorianCalendar(2010, 0, 2).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());
        long oneDay = 1000 * 60 * 60 * 24;
        assertEquals(oneDay, time.timeRadius);

        time = handler.getTime("insitu_WMOID_11851_20110101_20130101.nc");
        test = new GregorianCalendar(2012, 0, 1, 12, 0).getTime();
        assertEquals(test.getTime(), time.centralTime.getTime());

        time = handler.getTime("insitu_WMOID_11851_20110101_20110111.nc");
        long threeHundredAndSixtyFiveDays = oneDay * 5;
        assertEquals(threeHundredAndSixtyFiveDays, time.timeRadius);
    }

    @Test
    public void testFits() throws Exception {
        final double julianDateOf_2007_11_23_172624 = 2454428.185;

        Date testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_120000_000");
        assertTrue(handler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071124_020000_000");
        assertTrue(handler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_020000_000");
        assertFalse(handler.fits(testTime, julianDateOf_2007_11_23_172624));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071124_060000_000");
        assertFalse(handler.fits(testTime, julianDateOf_2007_11_23_172624));

        final double julianDateOf_2008_01_11_222721 = 2454477.394;

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_220000_000");
        assertTrue(handler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_110000_000");
        assertTrue(handler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_080000_000");
        assertFalse(handler.fits(testTime, julianDateOf_2008_01_11_222721));

        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20101124_050000_000");
        assertFalse(handler.fits(testTime, julianDateOf_2008_01_11_222721));
    }

    @Test
    public void testCreateFilledArray() throws Exception {
        Array fillArray = handler.createFillArray(DataType.INT, 15, new int[]{2, 3, 4});
        assertEquals(3, fillArray.getShape().length);
        assertEquals(24, fillArray.getSize());
        long size = fillArray.getSize();
        for(int i = 0; i < size; i++) {
            Object value = fillArray.getInt(i);
            assertEquals(15, value);
        }

        fillArray = handler.createFillArray(DataType.DOUBLE, Double.NEGATIVE_INFINITY, new int[]{5, 6, 7});
        assertEquals(3, fillArray.getShape().length);
        assertEquals(210, fillArray.getSize());
        size = fillArray.getSize();
        for(int i = 0; i < size; i++) {
            Object value = fillArray.getDouble(i);
            assertEquals(Double.NEGATIVE_INFINITY, value);
        }

    }
}
