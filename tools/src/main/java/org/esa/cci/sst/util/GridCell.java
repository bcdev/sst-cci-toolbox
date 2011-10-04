package org.esa.cci.sst.util;

// todo - make it applicable to multiple variables

/**
 * A grid box.
 *
 * @author Norman Fomferra
 */
public class GridCell {
    double sumX;
    double sumXX;
    long n;
    long nTotal;

    public void aggregate(double sum, long n) {
        this.sumX += sum;
        this.sumXX += sum * sum;
        this.n++;
        this.nTotal += n;
    }

    public void aggregate(GridCell gridCell) {
        if (gridCell.n > 0) {
            aggregate(gridCell.getMean(), gridCell.n);
        }
    }

    public double getMean() {
        return n > 0 ? sumX / n : 0.0;
    }
}
