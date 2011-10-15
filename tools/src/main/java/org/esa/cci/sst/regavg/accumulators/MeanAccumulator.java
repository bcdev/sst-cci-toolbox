package org.esa.cci.sst.regavg.accumulators;

import org.esa.cci.sst.regavg.Accumulator;

/**
 * A cell of a {@link org.esa.cci.sst.util.CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class MeanAccumulator extends Accumulator {

    private double sampleSum;
    private double sampleSqrSum;
    private double weightSum;
    private double weightSqrSum;

    @Override
    public void accumulate(double sample, double weight, long n) {
        if (!Double.isNaN(sample) && n > 0) {
            final double weightedSample = weight * sample;
            this.sampleSum += weightedSample;
            this.sampleSqrSum += weightedSample * weightedSample;
            this.weightSum += weight;
            this.weightSqrSum += weight * weight;
            this.accuCount++;
            this.totalCount += n;
        }
    }

    @Override
    public double getMean() {
        if (weightSum > 0.0) {
            return sampleSum / weightSum;
        } else {
            return Double.NaN;
        }
    }

    public double getSigma() {
        if (weightSum > 0.0) {
            final double mean = sampleSum / weightSum;
            final double sigmaSqr = sampleSqrSum / weightSqrSum - mean * mean;
            return sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;
        } else {
            return Double.NaN;
        }
    }

    public double getWeightSum() {
        return weightSum;
    }
}
