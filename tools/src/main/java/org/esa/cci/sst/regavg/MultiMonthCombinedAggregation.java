package org.esa.cci.sst.regavg;

/**
 * A multi-month (seasonal, annual) / combined aggregation
 * that aggregates monthly / combined cells ({@link CombinedAggregation}).
 *
 * @author Norman Fomferra
 */
public interface MultiMonthCombinedAggregation<A extends CombinedAggregation> extends CombinedAggregation {
    void accumulate(A aggregation);
}
