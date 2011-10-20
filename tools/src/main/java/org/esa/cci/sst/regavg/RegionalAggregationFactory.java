package org.esa.cci.sst.regavg;

/**
 * A factory for {@link RegionalAggregation}s.
 *@author Norman
 */
public interface RegionalAggregationFactory<A extends RegionalAggregation> {
    A createRegionalAggregation();
}
