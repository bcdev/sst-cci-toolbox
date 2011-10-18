package org.esa.cci.sst.util.accumulators;

import org.esa.cci.sst.util.Accumulator;

/**
 * An {@link Accumulator} used for weighted, random uncertainty averaging.
 *
 * @author Norman Fomferra
 */
public class RandomUncertaintyAccumulator extends Accumulator {

    private double sampleSqrSum;
    private double weightSqrSum;
    private long sampleCount;

    @Override
    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double weightedSample = weight * sample;
        sampleSqrSum += weightedSample * weightedSample;
        weightSqrSum += weight * weight;
    }

    @Override
    protected void accumulateAccumulator(Accumulator accumulator) {
        RandomUncertaintyAccumulator randomUncertaintyAccumulator = (RandomUncertaintyAccumulator) accumulator;
        sampleSqrSum += randomUncertaintyAccumulator.sampleSqrSum;
        weightSqrSum += randomUncertaintyAccumulator.weightSqrSum;
        sampleCount += randomUncertaintyAccumulator.sampleCount;
    }

    @Override
    public double computeAverage() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sampleSqrSum == 0.0) {
            return 0.0;
        }
        if (weightSqrSum == 0.0) {
            return Double.NaN;
        }
        final double meanSqr = sampleSqrSum / weightSqrSum;
        return meanSqr > 0.0 ? Math.sqrt(meanSqr) : 0.0;
    }

}
