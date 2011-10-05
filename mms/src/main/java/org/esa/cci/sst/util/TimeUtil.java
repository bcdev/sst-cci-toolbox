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

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Timeable;
import org.esa.cci.sst.tools.ToolException;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtil {

    /**
     * The Julian Date of 1978-01-01 00:00:00Z.
     */
    public static final double EPOCH_JD = 2440587.5;
    /**
     * The number of milliseconds per day.
     */
    public static final double MILLIS_PER_DAY = 86400000.0;
    /**
     * The number of milliseconds from 1970-01-01 00:00:00Z to 1978-01-01 00:00:00Z
     */
    public static final long MILLIS_1978 = createCalendar(1978, 0, 1, 0, 0, 0).getTimeInMillis();
    /**
     * The number of milliseconds from 1970-01-01 00:00:00Z to 1981-01-01 00:00:00Z
     */
    public static final long MILLIS_1981 = createCalendar(1981, 0, 1, 0, 0, 0).getTimeInMillis();
    /**
     * The number of seconds per day.
     */
    public static final double SECONDS_PER_DAY = 86400.0;

    private static final SimpleDateFormat CCSDS_UTC_MILLIS_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat CCSDS_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat COMPACT_UTC_FORMAT = new SimpleDateFormat(
            "yyyyMMddHHmmss");
    private static final SimpleDateFormat CCSDS_LOCAL_WITHOUT_T_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss,SSS");
    private static final SimpleDateFormat DAY_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd");

    static {
        CCSDS_UTC_MILLIS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        CCSDS_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        COMPACT_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DAY_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private TimeUtil() {
        // prevent instantiation
    }

    public static String formatCompactUtcFormat(Date time) {
        if (time == null) {
            return "";
        }
        return COMPACT_UTC_FORMAT.format(time);
    }

    public static String formatCcsdsUtcFormat(Date time) {
        if (time == null) {
            return "";
        }
        return CCSDS_UTC_FORMAT.format(time);
    }

    public static String formatCcsdsUtcMillisFormat(Date time) {
        if (time == null) {
            return "";
        }
        return CCSDS_UTC_MILLIS_FORMAT.format(time);
    }

    public static Date parseCcsdsUtcFormat(String timeString) throws ParseException {
        if (timeString.length() == 20) {
            return CCSDS_UTC_FORMAT.parse(timeString);
        }
        return CCSDS_UTC_MILLIS_FORMAT.parse(timeString);
    }

    public static Date julianDateToDate(double julianDate) {
        return new Date((long) ((julianDate - EPOCH_JD) * MILLIS_PER_DAY));
    }

    public static double dateToSecondsSinceEpoch(Date date) {
        return (date.getTime() - MILLIS_1978) / 1000.0;
    }

    public static double julianDateToSecondsSinceEpoch(double julianDate) {
        return (julianDate - EPOCH_JD) * SECONDS_PER_DAY - MILLIS_1978 / 1000.0;
    }

    public static double toJulianDate(Date date) {
        return date.getTime() / MILLIS_PER_DAY + EPOCH_JD;
    }

    public static Date secondsSince1981ToDate(double secondsSince1981) {
        return new Date(MILLIS_1981 + (long) (secondsSince1981 * 1000.0));
    }

    public static Date secondsSince1978ToDate(double secondsSince1978) {
        return new Date(MILLIS_1978 + (long) (secondsSince1978 * 1000.0));
    }

    public static double secondsSince1981ToSecondsSinceEpoch(double secondsSince1981) {
        return secondsSince1981 + (MILLIS_1981 - MILLIS_1978) / 1000.0;
    }

    public static double millisToSecondsSinceEpoch(long millis) {
        return (millis - MILLIS_1978) / 1000.0;
    }

    public static int intMillisToSecondsSinceEpoch(long millis) {
        return (int) ((millis - MILLIS_1978) / 1000);
    }

    /**
     * Checks if an objective time falls inside a time interval, while taking into account
     * a certain tolerance.
     *
     * @param time      The objective time.
     * @param start     The start of the time interval.
     * @param end       The end of the time interval.
     * @param timeDelta The tolerance (seconds).
     *
     * @return {@code true} if the objective time falls within the time interval (taking into
     *         account the tolerance), {@code false} otherwise.
     */
    public static boolean checkTimeOverlap(Date time, Date start, Date end, double timeDelta) {
        final double deltaInMillis = timeDelta * 1000.0;
        return time.getTime() + deltaInMillis >= start.getTime() &&
               time.getTime() - deltaInMillis < end.getTime();
    }

    public static double timeDifferenceInSeconds(Matchup m, Timeable t) {
        return timeDifferenceInSeconds(m.getRefObs().getTime(), t.getTime());
    }

    public static double timeDifferenceInSeconds(Date d, Date d2) {
        return Math.abs(d.getTime() - d2.getTime()) / 1000.0;
    }

    public static Date centerTime(Date startTime, Date endTime) {
        return new Date((startTime.getTime() + endTime.getTime()) / 2);
    }

    public static double timeRadius(Date startTime, Date endTime) {
        return Math.abs(endTime.getTime() - startTime.getTime()) / 2000.0;
    }

    private static Calendar createCalendar(int year, int month, int date, int hour, int minute, int second) {
        final Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        c.clear();
        c.set(year, month, date, hour, minute, second);
        return c;
    }

    public static Date getConfiguredTimeOf(String time) {
        final Date date;
        try {
            date = parseCcsdsUtcFormat(time);
        } catch (ParseException e) {
            final String message = MessageFormat.format("Unable to parse time string ''{0}''.", time);
            throw new ToolException(message, e, ToolException.CONFIGURATION_FILE_IO_ERROR);
        }
        return date;
    }

    @Deprecated
    public static int secondsSince1981ToSecondsSince1978(int startTime) {
        return (int) secondsSince1981ToSecondsSinceEpoch(startTime);
    }

    public static String dateOf(Date time) {
        if (time == null) {
            return "";
        }
        return DAY_UTC_FORMAT.format(time);
    }

    public static GregorianCalendar calendarDayOf(Date time) {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(time);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar;
    }
}
