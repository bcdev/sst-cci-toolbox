package org.esa.cci.sst.common.cell;

/**
 * {@author Bettina Scholze}
 * Date: 15.08.12 10:04
 */
public interface TemporalAggregationCell extends AggregationCell {

    void accumulate(Number[] values, double weight);
}
