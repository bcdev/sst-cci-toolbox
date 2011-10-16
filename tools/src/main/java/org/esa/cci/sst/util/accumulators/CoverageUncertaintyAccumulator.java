package org.esa.cci.sst.util.accumulators;

import org.esa.cci.sst.util.Accumulator;

/**
 * Sampling/coverage uncertainty, u(f), is calculated for each monthly 5° average,
 * combining entries in a look up table (s0 and p from LUT1) and the fraction of
 * the grid box that is observed, f.
 *
 * @author Norman Fomferra
 */
public class CoverageUncertaintyAccumulator extends Accumulator {

    private final double s0;
    private final double p;

    public CoverageUncertaintyAccumulator(double s0, double p) {
        this.s0 = s0;
        this.p = p;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
    }

    @Override
    public double computeAverage() {
        double f = sampleCount / 77500.0;
        return s0 * (1.0 - Math.pow(f, p));
    }
}
