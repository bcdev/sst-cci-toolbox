package org.esa.cci.sst.util;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator implements Cloneable {

    protected double weight;
    protected long sampleCount;
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

    public long getSampleCount() {
        return sampleCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public abstract double computeAverage();

    public void accumulateSample(double sample, double weight, long n) {
        if (isValidInput(sample, weight, n)) {
            accumulateSample(sample, weight);
            this.sampleCount++;
            this.totalCount += n;
        }
    }

    protected boolean isValidInput(double sample, double weight, long n) {
        return n > 0 && !Double.isNaN(sample) && !Double.isNaN(weight);
    }


    protected abstract void accumulateSample(double sample, double weight);

    public void accumulate(Accumulator accumulator) {
        if (accumulator.getSampleCount() > 0) {
            accumulateSample(accumulator.computeAverage(), accumulator.getWeight(), accumulator.getTotalCount());
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
