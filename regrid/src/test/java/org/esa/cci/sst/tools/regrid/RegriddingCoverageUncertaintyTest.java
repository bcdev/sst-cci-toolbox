package org.esa.cci.sst.tools.regrid;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static junit.framework.Assert.assertEquals;

/**
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class RegriddingCoverageUncertaintyTest {

    @Test
    public void testCalculateXDay() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        Date date1;
        Date date2;

        calendar.set(2010, Calendar.APRIL, 11);
        date1 = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        date2 = calendar.getTime();
        assertEquals(30.0, RegriddingCoverageUncertaintyProvider.calculateXDay(date1, date2));

        calendar.set(2012, Calendar.FEBRUARY, 7);
        date1 = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        date2 = calendar.getTime();
        assertEquals(1.0, RegriddingCoverageUncertaintyProvider.calculateXDay(date1, date2));

        calendar.set(2013, Calendar.FEBRUARY, 2);
        date1 = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        date2 = calendar.getTime();
        assertEquals(5.0, RegriddingCoverageUncertaintyProvider.calculateXDay(date1, date2));
    }
}
