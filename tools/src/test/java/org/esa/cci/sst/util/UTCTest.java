package org.esa.cci.sst.util;

import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class UTCTest {

    private static final DateFormat DATE_FORMAT = UTC.getDateFormat("yyyy-MM-dd");

    @Test
    public void testCalendarAddMonthly() throws Exception {
        Date date = parse("2004-01-01");
        Calendar calendar = UTC.createCalendar(date);
        assertEquals(parse("2004-01-01"), calendar.getTime());
        calendar.add(Calendar.MONTH, 1);
        assertEquals(parse("2004-02-01"), calendar.getTime());
        calendar.add(Calendar.MONTH, 1);
        assertEquals(parse("2004-03-01"), calendar.getTime());
        calendar.add(Calendar.MONTH, 1);
        assertEquals(parse("2004-04-01"), calendar.getTime());
        calendar.add(Calendar.MONTH, 9);
        assertEquals(parse("2005-01-01"), calendar.getTime());
    }

    @Test
    public void testCalendarAddDaily() throws Exception {
        Date date = parse("2004-02-28");
        Calendar calendar = UTC.createCalendar(date);
        assertEquals(parse("2004-02-28"), calendar.getTime());
        calendar.add(Calendar.DATE, 1);
        assertEquals(parse("2004-02-29"), calendar.getTime());
        calendar.add(Calendar.DATE, 1);
        assertEquals(parse("2004-03-01"), calendar.getTime());
        calendar.add(Calendar.DATE, 1);
        assertEquals(parse("2004-03-02"), calendar.getTime());
        calendar.add(Calendar.DATE, 1);
        assertEquals(parse("2004-03-03"), calendar.getTime());
    }

    @Test
    public void testGetDayOfYear() throws Exception {
        assertEquals(1, UTC.getDayOfYear(parse("2011-01-01")));
        assertEquals(32, UTC.getDayOfYear(parse("2011-02-01")));
        assertEquals(60, UTC.getDayOfYear(parse("2011-03-01")));
        assertEquals(365, UTC.getDayOfYear(parse("2011-12-31")));

        assertEquals(1, UTC.getDayOfYear(parse("2004-01-01")));
        assertEquals(32, UTC.getDayOfYear(parse("2004-02-01")));
        assertEquals(61, UTC.getDayOfYear(parse("2004-03-01")));
        assertEquals(366, UTC.getDayOfYear(parse("2004-12-31")));
    }

    private static Date parse(String dateString) throws ParseException {
        return DATE_FORMAT.parse(dateString);
    }
}
