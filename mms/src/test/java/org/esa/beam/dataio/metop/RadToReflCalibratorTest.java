package org.esa.beam.dataio.metop;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RadToReflCalibratorTest {

    private RadToReflCalibrator calibrator;

    @Before
    public void setUp() {
        calibrator = new RadToReflCalibrator(10.0, 15.0, 20.0);
    }

    @Test
    public void testGetConversionFactor() {
        assertEquals(0.0000375, calibrator.getConversionFactor(), 1e-8);
    }

    @Test
    public void testCalibrate() {
        final float calibrated = calibrator.calibrate(2000.f);

        assertEquals(5.3333332E7, calibrated, 1e-8);
    }
}
