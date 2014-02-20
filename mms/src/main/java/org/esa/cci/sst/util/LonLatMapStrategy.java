package org.esa.cci.sst.util;

import java.util.List;

class LonLatMapStrategy implements MapStrategy {

    private final int width;
    private final int height;

    LonLatMapStrategy(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void initialize(List<SamplingPoint> samplingPoints) {
        // nothing to do here
    }

    public PlotPoint map(SamplingPoint samplingPoint) {
        final double x = (samplingPoint.getLon() + 180.0) / 360.0;
        final double y = (90.0 - samplingPoint.getLat()) / 180.0;
        final int i = (int) (y * height);
        final int k = (int) (x * width);
        return new PlotPoint(k, i);
    }
}
