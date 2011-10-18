package org.esa.cci.sst.util;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator implements Cloneable {

    protected Accumulator() {
    }

    public void accumulate(double sample, double weight) {
        if (!Double.isNaN(sample) && !Double.isNaN(weight)) {
            accumulateSample(sample, weight);
        }
    }

    public void accumulate(Accumulator accumulator) {
        if (accumulator.getSampleCount() > 0) {
            accumulateAccumulator(accumulator);
        }
    }

    public void accumulateAverage(Accumulator accumulator, double weight) {
        if (accumulator.getSampleCount() > 0) {
            accumulate(accumulator.computeAverage(), weight);
        }
    }

    public abstract long getSampleCount();

    public abstract double computeAverage();

    protected abstract void accumulateSample(double sample, double weight);

    protected abstract void accumulateAccumulator(Accumulator accumulator);

    @Override
    public Accumulator clone() {
        try {
            return (Accumulator) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
