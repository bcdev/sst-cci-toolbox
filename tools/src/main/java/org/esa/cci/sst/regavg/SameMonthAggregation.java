package org.esa.cci.sst.regavg;

/**
 * A "same month" (daily, monthly) / regional aggregation
 * that aggregates daily, monthly / 5º,90º cells ({@link AggregationCell5}, {@link AggregationCell90}).
 *
 * @author Norman Fomferra
 */
public interface SameMonthAggregation<A extends AggregationCell> extends RegionalAggregation {
    void accumulate(A cell, double seaCoverage);
}
