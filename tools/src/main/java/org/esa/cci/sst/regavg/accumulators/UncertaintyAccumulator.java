package org.esa.cci.sst.regavg.accumulators;

import org.esa.cci.sst.regavg.Accumulator;

/**
 * A cell of a {@link org.esa.cci.sst.util.CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class UncertaintyAccumulator extends Accumulator {

    private double sampleSqrSum;
    private double weightSqrSum;

    @Override
    public void accumulate(double sample, double weight, long n) {
        if (!Double.isNaN(sample) && n > 0) {
            final double weightedSample = weight * sample;
            this.sampleSqrSum += weightedSample * weightedSample;
            this.weightSqrSum += weight * weight;
            this.accuCount++;
            this.totalCount += n;
        }
    }

    @Override
    public double getMean() {
         if (weightSqrSum > 0.0) {
            final double meanSqr = sampleSqrSum / weightSqrSum;
            return meanSqr > 0.0 ? Math.sqrt(meanSqr) : 0.0;
        } else {
            return Double.NaN;
        }
    }

}
