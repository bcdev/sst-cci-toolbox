package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.calculator.CoverageUncertainty;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

class MockCoverageUncertainty implements CoverageUncertainty {

    private final double magnitude90;
    private final double magnitude5;
    private final double exponent5;

    MockCoverageUncertainty(double magnitude90, double magnitude5, double exponent5) {
        this.magnitude90 = magnitude90;
        this.magnitude5 = magnitude5;
        this.exponent5 = exponent5;
    }

    @Override
    public final double calculate(int cellX, int cellY, long n, double resolution) {
        if (resolution == 5.0) {
            if (n == 0L) {
                return Double.NaN;
            } else {
                return magnitude5 * (1.0 - pow(n / 77500.0, exponent5));
            }
        } else if (resolution == 90.0) {
            if (n == 0L) {
                return Double.NaN;
            } else {
                return magnitude90 / sqrt(n);
            }
        } else {
            return 0.0;
        }
    }
}
