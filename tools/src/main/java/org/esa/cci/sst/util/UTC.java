package org.esa.cci.sst.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility methods for the UTC time zone.
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
     * @param pattern The data format pattern
     * @return A date format
     */
    public static DateFormat getDateFormat(String pattern) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, LOCALE);
        dateFormat.setCalendar(createCalendar());
        return dateFormat;
    }

    /**
     * @return A new Gregorian calendar
     */
    public static Calendar createCalendar() {
        return new GregorianCalendar(TIME_ZONE, Locale.ENGLISH);
    }

    /**
     * @return A new Gregorian calendar initialised to January, 1st of the given year.
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
     * @param utc The UTC date
     * @return A new Gregorian calendar initialised to the given UTC date.
     */
    public static Calendar createCalendar(Date utc) {
        final Calendar calendar = createCalendar();
        calendar.clear();
        calendar.setTime(utc);
        return calendar;
    }

    /**
     * @param utc the UTC date
     * @return The day of the year of the given UTC date. Ranges from to 1 to 366.
     */
    public static int getDayOfYear(Date utc) {
        Calendar calendar = createCalendar(utc);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return dayOfYear;
    }
}
