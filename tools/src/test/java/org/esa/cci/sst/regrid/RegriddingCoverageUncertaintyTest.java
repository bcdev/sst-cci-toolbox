package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.TemporalResolution;
import org.junit.Test;

import java.util.Calendar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * {@author Bettina Scholze}
 * Date: 21.11.12 14:39
 */
public class RegriddingCoverageUncertaintyTest {

    @Test
    public void testCalculateXDay_monthly() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.MONTHLY;
        final Calendar calendar = Calendar.getInstance();

        calendar.set(2010, Calendar.APRIL, 11);
        assertEquals(30.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution,
                                                                       calendar.getTime()));

        calendar.set(2012, Calendar.FEBRUARY, 7);
        assertEquals(29.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution,
                                                                       calendar.getTime()));

        calendar.set(2013, Calendar.FEBRUARY, 2);
        assertEquals(28.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution,
                                                                       calendar.getTime()));
    }

    @Test
    public void testCalculateXDay_daily() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.DAILY;
        assertEquals(0.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution, null));
    }

    @Test
    public void testCalculateXDay_seasonal() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.SEASONAL;
        try {
            RegriddingCoverageUncertainty.calculateXDay(temporalResolution, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCalculateXDay_annual() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.ANNUAL;
        try {
            RegriddingCoverageUncertainty.calculateXDay(temporalResolution, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }
}
