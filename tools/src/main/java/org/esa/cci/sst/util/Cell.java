package org.esa.cci.sst.util;

/**
 * A cell of a {@link CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class Cell implements Cloneable {
    private double sumX;
    private double sumXX;
    private long accuCount;
    private long totalCount;

    public void accumulate(double sample) {
        accumulate(sample, 1L);
    }

    public void accumulate(double sample, long n) {
        if (!Double.isNaN(sample) && n > 0) {
            this.sumX += sample;
            this.sumXX += sample * sample;
            this.accuCount++;
            this.totalCount += n;
        }
    }

    public void accumulate(Cell cell) {
        if (cell.accuCount > 0) {
            accumulate(cell.getMean(), cell.totalCount);
        }
    }

    public double getMean() {
        if (accuCount > 0) {
            return sumX / accuCount;
        } else {
            return Double.NaN;
        }
    }

    public double getSigma() {
        if (accuCount > 0) {
            final double mean = sumX / accuCount;
            final double sigmaSqr = sumXX / accuCount - mean * mean;
            return sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;
        } else {
            return Double.NaN;
        }
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
