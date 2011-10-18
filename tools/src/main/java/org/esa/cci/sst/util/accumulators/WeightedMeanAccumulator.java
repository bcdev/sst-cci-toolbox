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
    protected long sampleCount;

    @Override
    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double weightedSample = weight * sample;
        sampleSum += weightedSample;
        weightSum += weight;
        sampleCount++;
    }

    @Override
    protected void accumulateAccumulator(Accumulator accumulator) {
        WeightedMeanAccumulator meanAccumulator = (WeightedMeanAccumulator) accumulator;
        sampleSum += meanAccumulator.sampleSum;
        weightSum += meanAccumulator.weightSum;
        sampleCount += meanAccumulator.sampleCount;
    }

    @Override
    public double computeAverage() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sampleSum == 0.0) {
            return 0.0;
        }
        if (weightSum == 0.0) {
            return Double.NaN;
        }
        return sampleSum / weightSum;
    }
}
