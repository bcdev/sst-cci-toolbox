package org.esa.cci.sst.regavg;

/**
 * A factory for {@link CombinedAggregation}s.
 *@author Norman
 */
public interface CombinedAggregationFactory<A extends CombinedAggregation> {
    A createCombinedAggregation();
}
