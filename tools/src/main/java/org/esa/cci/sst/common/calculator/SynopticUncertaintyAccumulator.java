package org.esa.cci.sst.common.calculator;

/**
 * {@author Bettina Scholze}
 * Date: 06.09.12 14:41
 */
public class SynopticUncertaintyAccumulator extends NumberAccumulator {

    private double sumXX;
    private long sampleCount;

    @Override
    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        if (weight != 1.0){
            throw new IllegalArgumentException("is calculated non-weighted");
        }
        sumXX += sample * sample;
        sampleCount++;
    }

    @Override
    public double combine() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sumXX == 0.0) {
            return 0.0;
        }

        return Math.sqrt(sumXX);
    }
}
