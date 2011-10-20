package org.esa.cci.sst.regavg;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Access to coverage uncertainties.
 *
 * @author Norman Fomnferra
 */
public abstract class CoverageUncertaintyProvider {

    final int month;

    protected CoverageUncertaintyProvider(int month) {
        this.month = month;
    }

    /**
     * @param cellX The 90° cell X index.
     * @param cellY The 90° cell Y index.
     * @param n     The number of 5° grid boxes contributing to the 90° cell.
     * @return The coverage uncertainty for a 90° cell
     */
    public double getCoverageUncertainty90(int cellX, int cellY, long n) {
        if (n == 0L) {
            return Double.NaN;
        }
        final double s = getMagnitude90(cellX, cellY, month);
        return s / sqrt(n);
    }

    protected abstract double getMagnitude90(int cellX, int cellY, int month);

    /**
     * @param cellX The 5° cell X index.
     * @param cellY The 5° cell Y index.
     * @param n     The number of observations contributing to the 5° cell.
     * @return The coverage uncertainty for a 5° cell
     */
    public double getCoverageUncertainty5(int cellX, int cellY, long n) {
        if (n == 0L) {
            return Double.NaN;
        }
        final double s0 = getMagnitude5(cellX, cellY);
        final double p = getExponent5(cellX, cellY);
        final double f = n / 77500.0;
        return s0 * (1.0 - pow(f, p));
    }

    protected abstract double getMagnitude5(int cellX, int cellY);

    protected abstract double getExponent5(int cellX, int cellY);
}
