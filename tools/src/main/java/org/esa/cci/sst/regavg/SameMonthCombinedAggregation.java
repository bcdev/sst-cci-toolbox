package org.esa.cci.sst.regavg;

/**
 * A "same month" (daily, monthly) / combined aggregation
 * that aggregates daily, monthly / 5º,90º cells ({@link AggregationCell5}, {@link AggregationCell90}).
 *
 * @author Norman Fomferra
 */
public interface SameMonthCombinedAggregation<A extends AggregationCell> extends CombinedAggregation {
    void accumulate(A cell, double seaCoverage);
}
