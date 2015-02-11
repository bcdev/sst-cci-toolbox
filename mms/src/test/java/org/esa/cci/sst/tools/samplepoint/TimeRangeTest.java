package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class TimeRangeTest {

    @Test
    public void testIncludes() {
        final Date startDate = createDate(2005, 9, 14, 0, 0, 0);
        final Date endDate = createDate(2005, 9, 20, 23, 59, 59);

        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date date = createDate(2005, 9, 16, 13, 44, 9);
        assertTrue(timeRange.includes(date.getTime()));

        date = createDate(2006, 9, 16, 13, 44, 9);
        assertFalse(timeRange.includes(date.getTime()));

        date = createDate(2005, 9, 14, 0, 0, 1);
        assertTrue(timeRange.includes(date.getTime()));
        date = createDate(2005, 9, 13, 23, 59, 59);
        assertFalse(timeRange.includes(date.getTime()));

        date = createDate(2005, 9, 20, 23, 59, 58);
        assertTrue(timeRange.includes(date.getTime()));
        date = createDate(2005, 9, 21, 0, 0, 0);
        assertFalse(timeRange.includes(date.getTime()));
    }

    @Test
    public void testIntersectsWith_containedInRange() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2010, 5, 1, 0, 0, 0);
        Date compareStop = createDate(2010, 6, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.intersectsWith(contained));
    }

    @Test
    public void testIntersectsWith_outsideRange_before() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(1998, 5, 1, 0, 0, 0);
        Date compareStop = createDate(1998, 6, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertFalse(timeRange.intersectsWith(contained));
    }

    @Test
    public void testIntersectsWith_outsideRange_after() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2011, 5, 1, 0, 0, 0);
        Date compareStop = createDate(2011, 6, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertFalse(timeRange.intersectsWith(contained));
    }

    @Test
    public void testIntersectsWith_intersectingRange_beginning() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2009, 11, 12, 0, 0, 0);
        Date compareStop = createDate(2010, 2, 31, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.intersectsWith(contained));
    }

    @Test
    public void testIntersectsWith_intersectingRange_end() {
        final Date startDate = createDate(2010, 0, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 11, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2010, 11, 12, 0, 0, 0);
        Date compareStop = createDate(2011, 1, 1, 23, 59, 59);
        TimeRange contained = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.intersectsWith(contained));
    }

    @Test
    public void testIntersectsWith_containedInIntersectingRange() {
        final Date startDate = createDate(2010, 5, 1, 0, 0, 0);
        final Date endDate = createDate(2010, 5, 31, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        Date compareStart = createDate(2010, 2, 1, 0, 0, 0);
        Date compareStop = createDate(2010, 7, 31, 23, 59, 59);
        TimeRange containing = new TimeRange(compareStart, compareStop);

        assertTrue(timeRange.intersectsWith(containing));
    }

    @Test
    public void testGetCenterMonth() {
        final Date startDate = createDate(2010, 3, 13, 0, 0, 0);
        final Date endDate = createDate(2010, 5, 17, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        final TimeRange centerMonth = timeRange.getCenterMonth();
        assertNotNull(centerMonth);
        assertEquals(createDate(2010, 4, 1, 0, 0, 0).getTime(), centerMonth.getStartDate().getTime());
        assertEquals(createDate(2010, 5, 1, 0, 0, 0).getTime(), centerMonth.getStopDate().getTime());
    }

    @Test
    public void testGetMonthBefore() {
        final Date startDate = createDate(2010, 3, 13, 0, 0, 0);
        final Date endDate = createDate(2010, 5, 17, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        final TimeRange centerMonth = timeRange.getMonthBefore();
        assertNotNull(centerMonth);
        assertEquals(createDate(2010, 3, 1, 0, 0, 0).getTime(), centerMonth.getStartDate().getTime());
        assertEquals(createDate(2010, 4, 1, 0, 0, 0).getTime(), centerMonth.getStopDate().getTime());
    }

    @Test
    public void testGetMonthAfter() {
        final Date startDate = createDate(2010, 3, 13, 0, 0, 0);
        final Date endDate = createDate(2010, 5, 17, 23, 59, 59);
        final TimeRange timeRange = new TimeRange(startDate, endDate);

        final TimeRange centerMonth = timeRange.getMonthAfter();
        assertNotNull(centerMonth);
        assertEquals(createDate(2010, 5, 1, 0, 0, 0).getTime(), centerMonth.getStartDate().getTime());
        assertEquals(createDate(2010, 6, 1, 0, 0, 0).getTime(), centerMonth.getStopDate().getTime());
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
