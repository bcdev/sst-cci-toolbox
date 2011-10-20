package org.esa.cci.sst.regavg;

/**
 * An object that represents an aggregation of some given source "samples" and returns it
 * as a vector of numerical aggregation results.
 *
 * @author Norman Fomferra
 */
public interface Aggregation {
    long getSampleCount();
    Number[] getResults();
}
