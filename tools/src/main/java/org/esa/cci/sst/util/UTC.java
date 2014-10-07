/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
 * @author Ralf Quast
 */
public final class UTC {

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Locale LOCALE = Locale.ENGLISH;

    /**
     * Creates a date format using the given pattern. The date format returned, will use the
     * english locale ('en') and a calendar returned by the {@link #createCalendar()} method.
     *
     * @param pattern The data format pattern
     *
     * @return A date format
     */
    public static DateFormat getDateFormat(String pattern) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, LOCALE);
        dateFormat.setCalendar(createCalendar());
        return dateFormat;
    }

    /**
     * Creates a new Gregorian calaendar.
     *
     * @return a new Gregorian calendar
     */
    public static Calendar createCalendar() {
        return new GregorianCalendar(TIME_ZONE, Locale.ENGLISH);
    }

    /**
     * Create a new Gregorian calendar.
     *
     * @param year The calendar year used for initialization.
     *
     * @return a new Gregorian calendar initialised to January, 1st of the given year.
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
     * Create a new Gregorian calendar.
     *
     * @param utc The UTC date.
     *
     * @return A new Gregorian calendar initialised to the given UTC date.
     */
    public static Calendar createCalendar(Date utc) {
        final Calendar calendar = createCalendar();
        calendar.clear();
        calendar.setTime(utc);
        return calendar;
    }

    /**
     * Returns the day of year for a given UTC date.
     *
     * @param utc The UTC date.
     *
     * @return the day of the year of the given UTC date. Ranges from to 1 to 366.
     */
    public static int getDayOfYear(Date utc) {
        return createCalendar(utc).get(Calendar.DAY_OF_YEAR);
    }
}
