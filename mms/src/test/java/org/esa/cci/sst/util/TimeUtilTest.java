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

package org.esa.cci.sst.util;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Thomas Storm
 */
public class TimeUtilTest {

    @Test
    public void testJulianDateToDate() throws Exception {
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss_SSSzzz", Locale.ENGLISH);

        Date date;
        Date testTime;

        date = TimeUtil.julianDateToDate(2454428.185);
        testTime = format.parse("20071123_172624_000CET");
        assertEquals(testTime.getTime(), date.getTime());

        date = TimeUtil.julianDateToDate(2454477.394);
        testTime = format.parse("20080111_222721_599CET");
        assertEquals(testTime.getTime(), date.getTime());

        date = TimeUtil.julianDateToDate(2454115.05486);
        testTime = format.parse("20070114_141859_904CET");
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

    @Test
    public void testToSecondsSince1978() {
        Calendar calendar = createCalendar(1978, 0, 1, 0, 0, 0);
        assertEquals(0, TimeUtil.toSecondsSince1978(calendar.getTime()));

        calendar = createCalendar(1978, 0, 1, 0, 0, 10);
        assertEquals(10, TimeUtil.toSecondsSince1978(calendar.getTime()));

        calendar = createCalendar(1978, 0, 1, 0, 5, 10);
        assertEquals(310, TimeUtil.toSecondsSince1978(calendar.getTime()));

        calendar = createCalendar(1978, 5, 3, 0, 5, 22);
        assertEquals(13219522, TimeUtil.toSecondsSince1978(calendar.getTime()));
    }

    @Test
    public void testParseInsituFileNameDateFormat() throws ParseException {
        Date date = TimeUtil.parseInsituFileNameDateFormat("20060321");
        assertEquals(1142899200000L, date.getTime());

        date = TimeUtil.parseInsituFileNameDateFormat("20060322");
        assertEquals(1142985600000L, date.getTime());
    }

    @Test
    public void testToBeginningOfDay() {
        final Calendar calendar = createCalendar(2001, 5, 3, 6, 11, 22);
        final Date time = calendar.getTime();

        final Date beginning = TimeUtil.getBeginningOfDay(time);
        assertNotNull(beginning);

        calendar.setTime(beginning);
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToEndOfDay() {
        final Calendar calendar = createCalendar(2012, 11, 22, 18, 24, 53);
        final Date time = calendar.getTime();

        final Date end = TimeUtil.getEndOfDay(time);
        assertNotNull(end);

        calendar.setTime(end);
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, calendar.get(Calendar.MINUTE));
        assertEquals(59, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    private static Calendar createCalendar(int year, int month, int date, int hour, int minute, int second) {
        final GregorianCalendar c = createUtcCalendar();
        c.clear();
        c.set(year, month, date, hour, minute, second);
        return c;
    }

    private static GregorianCalendar createUtcCalendar() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
    }
}
