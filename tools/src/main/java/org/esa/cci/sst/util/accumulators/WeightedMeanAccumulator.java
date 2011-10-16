package org.esa.cci.sst.util.accumulators;

import org.esa.cci.sst.util.Accumulator;

/**
 * An {@link Accumulator} used for weighted mean averaging.
 *
 * @author Norman Fomferra
 */
public class WeightedMeanAccumulator extends Accumulator {

    private double sampleSum;
    private double weightSum;

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double weightedSample = weight * sample;
        this.sampleSum += weightedSample;
        this.weightSum += weight;
    }

    @Override
    public double computeAverage() {
        if (weightSum > 0.0) {
            return sampleSum / weightSum;
        } else {
            return Double.NaN;
        }
    }
}
