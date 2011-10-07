package org.esa.cci.sst.util;

/**
 * A cell of a {@link CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class Cell implements Cloneable {

    private double weight = 1.0;

    private double sampleSum;
    private double sampleSqrSum;
    private double weightSum;
    private double weightSqrSum;
    private long accuCount;
    private long totalCount;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void accumulate(double sample) {
        accumulate(sample, 1.0, 1L);
    }

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

    public void accumulate(Cell cell) {
        if (cell.accuCount > 0) {
            accumulate(cell.getSampleMean(), cell.getWeight(), cell.totalCount);
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

    public double getWeightSum() {
        return weightSum;
    }

    public long getAccuCount() {
        return accuCount;
    }

    public long getTotalCount() {
        return totalCount;
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
