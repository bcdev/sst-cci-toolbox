package org.esa.beam.dataio.metop;


import org.esa.beam.dataio.avhrr.calibration.RadianceCalibrator;

class RadToReflCalibrator implements RadianceCalibrator  {

    private final double conversionFactor;

    RadToReflCalibrator(double solarIrradiance) {
        this.conversionFactor = Math.PI / solarIrradiance;
    }

    @Override
    public float calibrate(float radiances) {
        return (float) (radiances * conversionFactor);
    }

    double getConversionFactor() {
        return conversionFactor;
    }
}
