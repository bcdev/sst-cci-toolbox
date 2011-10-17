package org.esa.cci.sst.util;

/**
 * A cell of a {@link CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class Cell implements Cloneable {

    private double sampleSum;
    private double sampleSqrSum;
    private double weightSum;
    private double weightSqrSum;
    private long sampleCount;

    public void accumulateSample(double sample, double weight) {
        if (!Double.isNaN(sample) && !Double.isNaN(weight)) {
            final double weightedSample = weight * sample;
            this.sampleSum += weightedSample;
            this.sampleSqrSum += weightedSample * weightedSample;
            this.weightSum += weight;
            this.weightSqrSum += weight * weight;
            this.sampleCount++;
        }
    }

    public void accumulateCellAverage(Cell cell, double weight) {
        if (cell.sampleCount > 0) {
            accumulateSample(cell.getSampleMean(), weight);
        }
    }

    public void accumulateCellSamples(Cell cell) {
        if (cell.sampleCount > 0) {
            this.sampleSum += cell.sampleSum;
            this.sampleSqrSum += cell.sampleSqrSum;
            this.weightSum += cell.weightSum;
            this.weightSqrSum += cell.weightSqrSum;
            this.sampleCount += cell.sampleCount;
        }
    }

    public double getSampleMean() {
        if (weightSum > 0.0) {
            return sampleSum / weightSum;
        } else {
            return Double.NaN;
        }
    }

    public double getSampleSigma() {
        if (weightSum > 0.0) {
            final double mean = sampleSum / weightSum;
            final double sigmaSqr = sampleSqrSum / weightSqrSum - mean * mean;
            return sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;
        } else {
            return Double.NaN;
        }
    }

    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    public Cell clone() {
        try {
            return (Cell) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
