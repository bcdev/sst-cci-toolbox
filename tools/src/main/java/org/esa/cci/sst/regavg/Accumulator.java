package org.esa.cci.sst.regavg;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator {

    private double weight;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public abstract void accumulate(double sample, double weight, long n);

    public abstract void accumulate(Accumulator agg);
}
