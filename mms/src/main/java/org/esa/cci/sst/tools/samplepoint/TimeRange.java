package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.TimeUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeRange {

    private Date startDate;
    private Date stopDate;

    public TimeRange(Date startDate, Date stopDate) {
        this.startDate = startDate;
        this.stopDate = stopDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public boolean includes(Date date) {
        final long startTime = startDate.getTime();
        final long stopTime = stopDate.getTime();
        final long actualTime = date.getTime();

        return (startTime <= actualTime && actualTime < stopTime);
    }

    public boolean intersectsWith(TimeRange other) {
        return includes(other.getStartDate()) || includes(other.getStopDate());
    }

    public TimeRange getCenterMonth() {
        final GregorianCalendar utcCalendar = getCalendarAtCenter();

        final Date beginningOfMonth = getBeginningOfMonth(utcCalendar);
        final Date endOfMonth = getEndOfMonth(utcCalendar);

        return new TimeRange(beginningOfMonth, endOfMonth);
    }

    public TimeRange getMonthBefore() {
        final GregorianCalendar utcCalendar = getCalendarAtCenter();
        utcCalendar.add(Calendar.MONTH, -1);

        final Date beginningOfMonth = getBeginningOfMonth(utcCalendar);
        final Date endOfMonth = getEndOfMonth(utcCalendar);

        return new TimeRange(beginningOfMonth, endOfMonth);
    }

    public TimeRange getMonthAfter() {
        final GregorianCalendar utcCalendar = getCalendarAtCenter();
        utcCalendar.add(Calendar.MONTH, 1);

        final Date beginningOfMonth = getBeginningOfMonth(utcCalendar);
        final Date endOfMonth = getEndOfMonth(utcCalendar);

        return new TimeRange(beginningOfMonth, endOfMonth);
    }

    private static Date getEndOfMonth(GregorianCalendar utcCalendar) {
        final int maxDayInMonth = utcCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        utcCalendar.set(Calendar.DAY_OF_MONTH, maxDayInMonth);
        final Date lastOfMonth = utcCalendar.getTime();
        return TimeUtil.getEndOfDay(lastOfMonth);
    }

    private static Date getBeginningOfMonth(GregorianCalendar utcCalendar) {
        utcCalendar.set(Calendar.DAY_OF_MONTH, 1);
        final Date firstOfMonth = utcCalendar.getTime();
        return TimeUtil.getBeginningOfDay(firstOfMonth);
    }

    private GregorianCalendar getCalendarAtCenter() {
        final Date centerDate = TimeUtil.getCenterTime(startDate, stopDate);
        final GregorianCalendar utcCalendar = TimeUtil.createUtcCalendar();
        utcCalendar.setTime(centerDate);
        return utcCalendar;
    }
}
