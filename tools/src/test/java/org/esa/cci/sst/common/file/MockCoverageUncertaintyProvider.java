package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.AggregationCell;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

final class MockCoverageUncertaintyProvider implements CoverageUncertaintyProvider {

    private final double magnitude90;
    private final double magnitude5;
    private final double exponent5;

    MockCoverageUncertaintyProvider(double magnitude90, double magnitude5, double exponent5) {
        this.magnitude90 = magnitude90;
        this.magnitude5 = magnitude5;
        this.exponent5 = exponent5;
    }

    @Override
    public double calculate(AggregationCell cell, double resolution) {
        return calculate(cell.getX(), cell.getY(), cell.getSampleCount(), resolution);
    }

    double calculate(int cellX, int cellY, long sampleCount, double resolution) {
        if (resolution == 5.0) {
            if (sampleCount == 0L) {
                return Double.NaN;
            } else {
                return magnitude5 * (1.0 - pow(sampleCount / 77500.0, exponent5));
            }
        } else if (resolution == 90.0) {
            if (sampleCount == 0L) {
                return Double.NaN;
            } else {
                return magnitude90 / sqrt(sampleCount);
            }
        } else {
            return 0.0;
        }
    }
}
