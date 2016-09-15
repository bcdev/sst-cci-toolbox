package org.esa.beam.dataio.metop;


import org.esa.beam.dataio.avhrr.calibration.RadianceCalibrator;

class RadToReflCalibrator implements RadianceCalibrator  {

    private final double inverseConversionFactor;

    RadToReflCalibrator(double equivalentWidth, double solarIrradiance, double earthSunDistance) {
        this.inverseConversionFactor = (100.0 * equivalentWidth * earthSunDistance * earthSunDistance) / solarIrradiance;
    }

    @Override
    public float calibrate(float radiances) {
        return (float) (radiances * inverseConversionFactor);
    }

    double getConversionFactor() {
        return 1.0 / inverseConversionFactor;
    }
}
