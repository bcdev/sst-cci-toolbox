/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import java.util.*;

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
    public void testSecondsSince1978ToDate() {
        final Date date = TimeUtil.secondsSince1978ToDate(791041746);

        final GregorianCalendar utcCalendar = createUtcCalendar();
        utcCalendar.setTime(date);

        assertEquals(2003, utcCalendar.get(Calendar.YEAR));
        assertEquals(0, utcCalendar.get(Calendar.MONTH));
        assertEquals(25, utcCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(13, utcCalendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(49, utcCalendar.get(Calendar.MINUTE));
        assertEquals(6, utcCalendar.get(Calendar.SECOND));
    }

    @Test
    public void testParseInsituFileNameDateFormat() throws ParseException {
        Date date = TimeUtil.parseInsituFileNameDateFormat("20060321");
        assertEquals(1142899200000L, date.getTime());

        date = TimeUtil.parseInsituFileNameDateFormat("20060322");
        assertEquals(1142985600000L, date.getTime());
    }

    @Test
    public void testFormatInsituFileNameFormat() {
        final Calendar calendar = createCalendar(2007, 8, 5, 10, 4, 33);
        final Date time = calendar.getTime();

        final String formatted = TimeUtil.formatInsituFilenameFormat(time);
        assertEquals("20070905", formatted);
    }

    @Test
    public void testParseShortUtcFormat() throws ParseException {
         Date date = TimeUtil.parseShortUtcFormat("2012-11-01");

        final GregorianCalendar calendar = createUtcCalendar();
        calendar.setTime(date);

        assertEquals(2012, calendar.get(Calendar.YEAR));
        assertEquals(11 - 1, calendar.get(Calendar.MONTH));
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testFormatShortUtcFormat() {
        final Calendar calendar = createCalendar(2006, 7, 4, 9, 3, 31);
        final Date time = calendar.getTime();

        final String formatted = TimeUtil.formatShortUtcFormat(time);
        assertEquals("2006-08-04", formatted);
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

    @Test
    public void testGetYear() {
        Calendar calendar = createCalendar(2012, 11, 22, 18, 24, 53);
        Date date = calendar.getTime();

        assertEquals(2012, TimeUtil.getYear(date));

        calendar = createCalendar(1998, 4, 9, 11, 17, 38);
        date = calendar.getTime();

        assertEquals(1998, TimeUtil.getYear(date));
    }

    @Test
    public void testGetMonth() {
        Calendar calendar = createCalendar(2011, 8, 19, 18, 24, 53);
        Date date = calendar.getTime();

        assertEquals(9, TimeUtil.getMonth(date));

        calendar = createCalendar(1999, 5, 12, 11, 17, 38);
        date = calendar.getTime();

        assertEquals(6, TimeUtil.getMonth(date));
    }

    @Test
    public void testGetBeginOfMoth() {
        Calendar calendar = createCalendar(2011, 8, 19, 18, 24, 53);
        Date date = calendar.getTime();

        final Date begin = TimeUtil.getBeginOfMonth(date);
        calendar.setTime(begin);
        assertEquals(2011, calendar.get(Calendar.YEAR));
        assertEquals(8, calendar.get(Calendar.MONTH));
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testGetEndOfMoth() {
        Calendar calendar = createCalendar(2010, 7, 14, 15, 24, 53);
        Date date = calendar.getTime();

        Date end = TimeUtil.getEndOfMonth(date);
        calendar.setTime(end);
        assertEquals(2010, calendar.get(Calendar.YEAR));
        assertEquals(7, calendar.get(Calendar.MONTH));
        assertEquals(31, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, calendar.get(Calendar.MINUTE));
        assertEquals(59, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));

        calendar = createCalendar(2010, 8, 14, 15, 24, 53);
        date = calendar.getTime();
        end = TimeUtil.getEndOfMonth(date);
        calendar.setTime(end);
        assertEquals(2010, calendar.get(Calendar.YEAR));
        assertEquals(8, calendar.get(Calendar.MONTH));
        assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, calendar.get(Calendar.MINUTE));
        assertEquals(59, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testGetTimeDifferenceInSeconds() {
        Date date_1 = new Date(10000010000L);
        Date date_2 = new Date(10000020000L);

        assertEquals(10.0, TimeUtil.getTimeDifferenceInSeconds(date_1, date_2), 1e-8);
        assertEquals(10.0, TimeUtil.getTimeDifferenceInSeconds(date_2, date_1), 1e-8);

        date_1 = new Date(23000022000L);
        date_2 = new Date(23000033000L);

        assertEquals(11.0, TimeUtil.getTimeDifferenceInSeconds(date_1, date_2), 1e-8);
        assertEquals(11.0, TimeUtil.getTimeDifferenceInSeconds(date_2, date_1), 1e-8);
    }

    @Test
    public void testFormatCcsdsUtcMillisFormat() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 46800000);

        assertEquals("2010-01-01T13:00:00.000Z", TimeUtil.formatCcsdsUtcMillisFormat(calendar.getTime()));
    }

    @Test
    public void testFormatCcsdsUtcMillisFormat_nullInput() {
        assertEquals("", TimeUtil.formatCcsdsUtcMillisFormat(null));
    }

    @Test
    public void testDateToSecondsSinceEpoch() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.DAY_OF_YEAR, 334);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 83019029);

        assertEquals(1038697419.029, TimeUtil.dateToSecondsSinceEpoch(calendar.getTime()), 1e-8);
    }

    @Test
    public void testFormatCcsdsUtcFormat() {
        final Calendar calendar = createCalendar(2012, 10, 23, 20, 46, 13);

        final String ccsdsUtcFormat = TimeUtil.formatCcsdsUtcFormat(calendar.getTime());
        assertEquals("2012-11-23T20:46:13Z", ccsdsUtcFormat);
    }

    @Test
    public void testFormatCcsdsUtcFormat_nullInput() {
        final String ccsdsUtcFormat = TimeUtil.formatCcsdsUtcFormat(null);
        assertEquals("", ccsdsUtcFormat);
    }

    @Test
    public void testFormatUtcIsoFormat() {
        final Calendar calendar = createCalendar(2013, 9, 22, 20, 46, 13);

        final String ccsdsUtcFormat = TimeUtil.formatIsoUtcFormat(calendar.getTime());
        assertEquals("2013-10-22T20:46:13", ccsdsUtcFormat);
    }

    @Test
    public void testSecondsSince1981ToDate() {
        Date date = TimeUtil.secondsSince1981ToDate(667849283);

        final GregorianCalendar utcCalendar = createUtcCalendar();
        utcCalendar.setTime(date);

        assertEquals(2002, utcCalendar.get(Calendar.YEAR));
        assertEquals(2, utcCalendar.get(Calendar.MONTH));
        assertEquals(1, utcCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, utcCalendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(41, utcCalendar.get(Calendar.MINUTE));
        assertEquals(23, utcCalendar.get(Calendar.SECOND));

        date = TimeUtil.secondsSince1981ToDate(0);
        utcCalendar.setTime(date);

        assertEquals(1981, utcCalendar.get(Calendar.YEAR));
        assertEquals(0, utcCalendar.get(Calendar.MONTH));
        assertEquals(1, utcCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, utcCalendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, utcCalendar.get(Calendar.MINUTE));
        assertEquals(0, utcCalendar.get(Calendar.SECOND));
    }

    @Test
    public void testCreateCalendarAtBeginningOfYear() {
        final Calendar calendar = TimeUtil.createCalendarAtBeginningOfYear(2005);
        assertNotNull(calendar);

        assertEquals(2005, calendar.get(Calendar.YEAR));
        assertEquals(0, calendar.get(Calendar.MONTH));
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testCreateUtcCalendarWithDate() {
        final Date date = new Date(69787284542L);

        final Calendar calendar = TimeUtil.createUtcCalendar(date);
        assertNotNull(calendar);

        assertEquals(1972, calendar.get(Calendar.YEAR));
        assertEquals(2, calendar.get(Calendar.MONTH));
        assertEquals(18, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(21, calendar.get(Calendar.MINUTE));
        assertEquals(24, calendar.get(Calendar.SECOND));
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
