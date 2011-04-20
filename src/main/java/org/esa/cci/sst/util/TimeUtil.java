package org.esa.cci.sst.util;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtil {

    public static final double MILLIS_PER_DAY = 86400000.0;
    public static final double SECONDS_PER_DAY = 86400.0;

    private static final SimpleDateFormat CCSDS_UTC_MILLIS_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat CCSDS_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat CCSDS_LOCAL_WITHOUT_T_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss,SSS");
    private static final double EPOCH_JD = 2440587.5;
    private static final long MILLIS_1978 = createCalendar(1978, 0, 1, 0, 0, 0).getTimeInMillis();
    private static final long MILLIS_1981 = createCalendar(1981, 0, 1, 0, 0, 0).getTimeInMillis();

    static {
        CCSDS_UTC_MILLIS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        CCSDS_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private TimeUtil() {
        // prevent instantiation
    }

    public static String formatCcsdsUtcFormat(Date time) {
        if (time == null) {
            return "";
        }
        return CCSDS_UTC_FORMAT.format(time);
    }

    public static long parseCcsdsUtcFormat(String timeString) throws ParseException {
        if (timeString.length() == 20) {
            return CCSDS_UTC_FORMAT.parse(timeString).getTime();
        }
        return CCSDS_UTC_MILLIS_FORMAT.parse(timeString).getTime();
    }

    public static Date julianDateToDate(double julianDate) {
        return new Date((long) ((julianDate - EPOCH_JD) * MILLIS_PER_DAY));
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

    public static double secondsSince1981ToSecondsSinceEpoch(double secondsSince1981) {
        return secondsSince1981 + (MILLIS_1981 - MILLIS_1978) / 1000.0;
    }

    public static double computeTimeDelta(final Matchup matchup, final Observation observation) {
        return Math.abs(matchup.getRefObs().getTime().getTime() - observation.getTime().getTime()) / 1000.0;
    }

    private static Calendar createCalendar(int year, int month, int date, int hour, int minute, int second) {
        final GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        c.clear();
        c.set(year, month, date, hour, minute, second);
        return c;
    }
}
