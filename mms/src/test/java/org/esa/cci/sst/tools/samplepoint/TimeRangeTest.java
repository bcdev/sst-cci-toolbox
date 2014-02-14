package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeRangeTest {

    @Test
    public void testIsWithin() {
        final Date startDate = createDate(2005, 9, 14, 0, 0, 0);
        final Date endDate = createDate(2005, 9, 20, 23, 59, 59);

        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date date = createDate(2005, 9, 16, 13, 44, 9);
        assertTrue(timeRange.isWithin(date));

        date = createDate(2006, 9, 16, 13, 44, 9);
        assertFalse(timeRange.isWithin(date));

        date = createDate(2005, 9, 14, 0, 0, 1);
        assertTrue(timeRange.isWithin(date));
        date = createDate(2005, 9, 13, 23, 59, 59);
        assertFalse(timeRange.isWithin(date));

        date = createDate(2005, 9, 20, 23, 59, 59);
        assertTrue(timeRange.isWithin(date));
        date = createDate(2005, 9, 21, 0, 0, 0);
        assertFalse(timeRange.isWithin(date));
    }

    @Test
    public void testHasIntersectWith_containedInRange() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2010, 5, 1, 0, 0, 0);
        Date compareStop = createDate(2010, 6, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.hasIntersectWith(contained));
    }

    @Test
    public void testHasIntersectWith_outsideRange() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(1998, 5, 1, 0, 0, 0);
        Date compareStop = createDate(1998, 6, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertFalse(timeRange.hasIntersectWith(contained));
    }

    @Test
    public void testHasIntersectWith_intersectingRange() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2009, 11, 12, 0, 0, 0);
        Date compareStop = createDate(2010, 2, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.hasIntersectWith(contained));
    }

    private Date createDate(int year, int month, int day, int hour, int minute, int second) {
        final GregorianCalendar calendar = TimeUtil.createUtcCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
