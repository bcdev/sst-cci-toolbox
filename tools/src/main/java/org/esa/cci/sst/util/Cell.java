package org.esa.cci.sst.util;

/**
 * A cell of a {@link CellGrid} used for simple (currently unweighted) mean averaging.
 *
 * @author Norman Fomferra
 */
public class Cell {
    private double sumX;
    private double sumXX;
    private long nAcc;
    private long nTotal;

    public void accumulate(double sample) {
        accumulate(sample, 1L);
    }

    public void accumulate(double sample, long n) {
        if (!Double.isNaN(sample) && n > 0) {
            this.sumX += sample;
            this.sumXX += sample * sample;
            this.nAcc++;
            this.nTotal += n;
        }
    }

    public void accumulate(Cell cell) {
        if (cell.nAcc > 0) {
            accumulate(cell.getMean(), cell.nTotal);
        }
    }

    public double getMean() {
        if (nAcc > 0) {
            return sumX / nAcc;
        } else {
            return Double.NaN;
        }
    }

    public double getSigma() {
        if (nAcc > 0) {
            final double mean = sumX / nAcc;
            final double sigmaSqr = sumXX / nAcc - mean * mean;
            return sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;
        } else {
            return Double.NaN;
        }
    }
}
