package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.SpatialResolution;
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
        final TemporalResolution temporalResolution = TemporalResolution.monthly;
        final Calendar calendar = Calendar.getInstance();

        calendar.set(2010, Calendar.APRIL, 11);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(30.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution));

        calendar.set(2012, Calendar.FEBRUARY, 7);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(29.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution));

        calendar.set(2013, Calendar.FEBRUARY, 2);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(28.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution));
    }

    @Test
    public void testCalculateXDay_daily() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.daily;
        assertEquals(0.0, RegriddingCoverageUncertainty.calculateXDay(temporalResolution));
    }

    @Test
    public void testCalculateXDay_seasonal() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.seasonal;
        try {
            RegriddingCoverageUncertainty.calculateXDay(temporalResolution);
            fail("Exception expected.");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCalculateXDay_annual() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.annual;
        try {
            RegriddingCoverageUncertainty.calculateXDay(temporalResolution);
            fail("Exception expected.");
        } catch (IllegalArgumentException expected) {
        }
    }
}
