package org.esa.cci.sst.util.accumulators;

import org.esa.cci.sst.util.Accumulator;

/**
 * An {@link Accumulator} used for weighted uncertainty averaging.
 *
 * @author Norman Fomferra
 */
public class UncertaintyAccumulator extends Accumulator {

    private double sampleSqrSum;
    private double weightSqrSum;

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double weightedSample = weight * sample;
        this.sampleSqrSum += weightedSample * weightedSample;
        this.weightSqrSum += weight * weight;
    }

    @Override
    public double computeAverage() {
        if (weightSqrSum > 0.0) {
            final double meanSqr = sampleSqrSum / weightSqrSum;
            return meanSqr > 0.0 ? Math.sqrt(meanSqr) : 0.0;
        } else {
            return Double.NaN;
        }
    }

}
