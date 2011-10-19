package org.esa.cci.sst.util;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator {

    protected Accumulator() {
    }

    public void accumulate(double sample, double weight) {
        if (!Double.isNaN(sample) && !Double.isNaN(weight)) {
            accumulateSample(sample, weight);
        }
    }

    public abstract long getSampleCount();

    public abstract double computeAverage();

    protected abstract void accumulateSample(double sample, double weight);
}
