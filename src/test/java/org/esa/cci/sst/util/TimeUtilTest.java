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

package org.esa.cci.sst.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class TimeUtilTest {

    @Test
    public void testJulianDateToDate() throws Exception {
        Date date = TimeUtil.julianDateToDate(2454428.185);
        Date testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20071123_172624_000");
        assertEquals(testTime, date);

        date = TimeUtil.julianDateToDate(2454477.394);
        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20080111_222721_599");
        assertEquals(testTime.getTime(), date.getTime());

        date = TimeUtil.julianDateToDate(2454115.05486);
        testTime = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").parse("20070114_141859_904");
        assertEquals(testTime.getTime(), date.getTime());
    }

    @Test
    public void testJulianDateToSecondsSinceEpoch_forEpoch() throws Exception {
        final Calendar epoch = createCalendar(1978, 0, 1, 0, 0, 0);
        final double julianDate = TimeUtil.toJulianDate(epoch.getTime());
        final double seconds = TimeUtil.julianDateToSecondsSinceEpoch(julianDate);

        assertEquals(0.0, seconds, 0.0);
    }

    @Test
    public void testJulianDateToSecondsSinceEpoch_forOtherDate() throws Exception {
        final Calendar oneDaySinceEpoch = createCalendar(1978, 0, 2, 0, 0, 0);
        final double julianDate = TimeUtil.toJulianDate(oneDaySinceEpoch.getTime());
        final double seconds = TimeUtil.julianDateToSecondsSinceEpoch(julianDate);

        assertEquals(TimeUtil.SECONDS_PER_DAY, seconds, 0.0);
    }

    @Test
    public void testSecondsSince1981ToSecondsSinceEpoch() throws Exception {
        final double seconds = TimeUtil.secondsSince1981ToSecondsSinceEpoch(0.0);

        assertEquals((3 * 365 + 1) * TimeUtil.SECONDS_PER_DAY, seconds, 0.0);
    }

    private static Calendar createCalendar(int year, int month, int date, int hour, int minute, int second) {
        final GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        c.clear();
        c.set(year, month, date, hour, minute, second);
        return c;
    }
}
