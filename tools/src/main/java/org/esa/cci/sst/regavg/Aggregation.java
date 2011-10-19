package org.esa.cci.sst.regavg;

/**
 * Some aggregation that produces a vector of numerical results.
 *
 * @author Norman Fomferra
 */
public interface Aggregation {
    long getSampleCount();
    Number[] getResults();
}
