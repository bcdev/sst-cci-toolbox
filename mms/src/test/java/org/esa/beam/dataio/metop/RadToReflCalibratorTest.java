package org.esa.beam.dataio.metop;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RadToReflCalibratorTest {

    private RadToReflCalibrator calibrator;

    @Before
    public void setUp() {
        calibrator = new RadToReflCalibrator(15.0);
    }

    @Test
    public void testGetConversionFactor() {
        assertEquals(0.20943951023931953, calibrator.getConversionFactor(), 1e-8);
    }

    @Test
    public void testCalibrate() {
        final float calibrated = calibrator.calibrate(2000.f);

        assertEquals(418.8790283203125, calibrated, 1e-8);
    }
}
