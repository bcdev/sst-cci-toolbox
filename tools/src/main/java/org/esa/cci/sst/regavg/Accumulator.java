package org.esa.cci.sst.regavg;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator implements Cloneable {

    protected double weight;
    protected long accuCount;
    protected long totalCount;

    protected Accumulator() {
        this.weight = 1.0;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public long getAccuCount() {
        return accuCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public abstract double getMean();

    public abstract void accumulate(double sample, double weight, long n);

    public void accumulate(Accumulator cell) {
        if (cell.getAccuCount() > 0) {
            accumulate(cell.getMean(), cell.getWeight(), cell.getTotalCount());
        }
    }

    @Override
    public Accumulator clone() {
        try {
            return (Accumulator) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
