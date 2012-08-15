package org.esa.cci.sst.util;

/**
 * {@author Bettina Scholze}
 * Date: 15.08.12 10:04
 */
public interface AccumulationCell extends AggregationCell {

    void accumulate(Number[] values, double weight);
}
