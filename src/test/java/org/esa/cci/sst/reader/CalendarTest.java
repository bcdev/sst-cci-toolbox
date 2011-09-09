package org.esa.cci.sst.reader;

import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class CalendarTest {
    @Test
    public void testMillisConversion() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 46800000);
        assertEquals("2010-01-01T13:00:00.000Z", TimeUtil.formatCcsdsUtcMillisFormat(calendar.getTime()));
    }
}
