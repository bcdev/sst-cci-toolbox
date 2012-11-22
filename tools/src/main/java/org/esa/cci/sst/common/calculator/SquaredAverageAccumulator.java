package org.esa.cci.sst.common.calculator;

/**
 * {@author Bettina Scholze}
 * Date: 21.11.12 13:31
 */
public class SquaredAverageAccumulator  extends NumberAccumulator {

    private double sumXX;
    private double sumW;
    private long sampleCount;

    @Override
    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double weightedSample = weight * sample;
        sumXX += weightedSample * weightedSample;
        sumW += weight;
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
        if (sumW == 0.0) {
            return Double.NaN;
        }
        final double weightedSqrSum = sumXX / (sumW * sumW);
        return weightedSqrSum > 0.0 ? weightedSqrSum : 0.0;
    }
}
