package org.esa.cci.sst.regavg;

/**
 * A daily or monthly / 90º cell that aggregates daily or monthly / 5º cells ({@link AggregationCell5}).
 *
 * @author Norman Fomferra
 */
public interface AggregationCell90<C extends AggregationCell5> extends AggregationCell {
    void accumulate(C cell, double seaCoverage90);
}
