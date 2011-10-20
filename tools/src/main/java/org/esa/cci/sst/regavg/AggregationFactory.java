package org.esa.cci.sst.regavg;

/**
 * A factory for {@link Aggregation}s.
 *@author Norman
 */
public interface AggregationFactory<A extends Aggregation> {
    A createAggregation();
}
