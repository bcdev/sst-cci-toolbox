package org.esa.cci.sst.common.calculator;

import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.junit.Test;

import java.util.Calendar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * {@author Bettina Scholze}
 * Date: 21.11.12 14:39
 */
public class CoverageUncertaintyForRegriddingTest {

    @Test
    public void testCalculateXSpace() throws Exception {
        CoverageUncertaintyForRegridding coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_10_00, null, null);
        assertEquals("diagonale in km", 1144.58, coverageUncertainty.calculateXKm(10, 10), 1e-02);

        coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_10_00, null, null);
        assertEquals("diagonale in km", 1568.52, coverageUncertainty.calculateXKm(0, 0), 1e-02);

        coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_1_00, null, null);
        assertEquals("diagonale in km", 155.94, coverageUncertainty.calculateXKm(10, 10), 1e-02);

        coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_1_00, null, null);
        assertEquals("diagonale in km", 113.02, coverageUncertainty.calculateXKm(1000, 1000), 1e-02);

        coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_0_05, null, null);
        assertEquals("diagonale in km", 7.86, coverageUncertainty.calculateXKm(10, 10), 1e-02);

        coverageUncertainty = new CoverageUncertaintyForRegridding(
                TemporalResolution.daily, SpatialResolution.DEGREE_0_05, null, null);
        assertEquals("diagonale in km", 6.61, coverageUncertainty.calculateXKm(5000, 1000), 1e-02);
    }

    @Test
    public void testCalculateXDay_monthly() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.monthly;
        final Calendar calendar = Calendar.getInstance();

        calendar.set(2010, Calendar.APRIL, 11);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(30.0, CoverageUncertaintyForRegridding.calculateXDay(temporalResolution));

        calendar.set(2012, Calendar.FEBRUARY, 7);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(29.0, CoverageUncertaintyForRegridding.calculateXDay(temporalResolution));

        calendar.set(2013, Calendar.FEBRUARY, 2);
        temporalResolution.setDate1(calendar.getTime());
        assertEquals(28.0, CoverageUncertaintyForRegridding.calculateXDay(temporalResolution));
    }

    @Test
    public void testCalculateXDay_daily() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.daily;
        assertEquals(0.0, CoverageUncertaintyForRegridding.calculateXDay(temporalResolution));
    }

    @Test
    public void testCalculateXDay_seasonal() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.seasonal;
        try {
            CoverageUncertaintyForRegridding.calculateXDay(temporalResolution);
            fail("Exception expected.");
        } catch (Exception expected) {
            assertEquals("temporalResolution must be 'daily' or 'monthly'", expected.getMessage());
        }
    }

    @Test
    public void testCalculateXDay_annual() throws Exception {
        final TemporalResolution temporalResolution = TemporalResolution.annual;
        try {
            CoverageUncertaintyForRegridding.calculateXDay(temporalResolution);
            fail("Exception expected.");
        } catch (Exception expected) {
            assertEquals("temporalResolution must be 'daily' or 'monthly'", expected.getMessage());
        }
    }
}
