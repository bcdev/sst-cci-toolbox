package org.esa.cci.sst.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility methods for the UTC time.
 *
 * @author Norman Fomferra
 */
public class UTC {

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final DateFormat ISO_FORMAT = getDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static DateFormat getIsoFormat() {
        return ISO_FORMAT;
    }

    /**
     * Creates a date format using the given pattern. The date format returned, will use the
     * english locale ('en') and a calendar returned by the {@link #createCalendar()} method.
     *
     * @param pattern the data format pattern
     * @return a date format
     */
    public static DateFormat getDateFormat(String pattern) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, LOCALE);
        dateFormat.setCalendar(createCalendar());
        return dateFormat;
    }

    /**
     * @return a new Gregorian calendar
     */
    public static Calendar createCalendar() {
        return new GregorianCalendar(TIME_ZONE, Locale.ENGLISH);
    }

    /**
     * @return a new Gregorian calendar
     */
    public static Calendar createCalendar(int year) {
        final Calendar calendar = createCalendar();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        return calendar;
    }

    /**
     * @param utc the UTC date
     * @return a new Gregorian calendar initialised to the given UTC date.
     */
    public static Calendar createCalendar(Date utc) {
        final Calendar calendar = createCalendar();
        calendar.clear();
        calendar.setTime(utc);
        return calendar;
    }
}
