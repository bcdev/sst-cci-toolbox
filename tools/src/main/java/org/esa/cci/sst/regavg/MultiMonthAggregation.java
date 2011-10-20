package org.esa.cci.sst.regavg;

/**
 * A "multi-month" (seasonal, annual) / regional aggregation
 * that aggregates monthly / regional aggregations ({@link RegionalAggregation}).
 *
 * @author Norman Fomferra
 */
public interface MultiMonthAggregation<A extends RegionalAggregation> extends RegionalAggregation {
    void accumulate(A aggregation);
}
