package org.esa.cci.sst.util;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class TimeUtil {

    private static final SimpleDateFormat CCSDS_UTC_MILLIS_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat CCSDS_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat CCSDS_LOCAL_WITHOUT_T_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss,SSS");
    private static final double JD_EPOCH = 2440587.5;
    private static final long MILLIS_1981;
    public static final double MILLIS_PER_DAY = 86400000.0;

    static {
        CCSDS_UTC_MILLIS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        CCSDS_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            MILLIS_1981 = TimeUtil.parseCcsdsUtcFormat("1981-01-01T00:00:00Z");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
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

    private static String formatCcsdsUtcFormat(long timeInMillis) {
        return CCSDS_UTC_FORMAT.format(new Date(timeInMillis));
    }

    private static long parseCcsdsLocalTimeWithoutT(String timeString) throws ParseException {
        return CCSDS_LOCAL_WITHOUT_T_FORMAT.parse(timeString).getTime();
    }

    public static long parseCcsdsUtcFormat(String timeString) throws ParseException {
        if (timeString.length() == "yyyy-MM-ddTHH:MM:ssZ".length()) {
            return CCSDS_UTC_FORMAT.parse(timeString).getTime();
        }
        return CCSDS_UTC_MILLIS_FORMAT.parse(timeString).getTime();
    }

    public static Date toDate(double julianDate) {
        return new Date((long) ((julianDate - JD_EPOCH) * MILLIS_PER_DAY));
    }

    public static double toJulianDate(Date date) {
        return date.getTime() / MILLIS_PER_DAY + JD_EPOCH;
    }

    public static Date secondsSince1981ToDate(double secondsSince1981) {
        return new Date(MILLIS_1981 + (long) (secondsSince1981 * 1000.0));
    }

    public static int computeTimeDelta(final Matchup matchup, final Observation observation) {
        return (int) Math.abs(
                    (matchup.getRefObs().getTime().getTime() - observation.getTime().getTime()) / 1000);
    }
}
