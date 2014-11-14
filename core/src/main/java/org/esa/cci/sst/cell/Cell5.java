package org.esa.cci.sst.cell;

import org.esa.cci.sst.aggregate.AggregationContext;

public final class Cell5 extends DefaultSpatialAggregationCell {

    public Cell5(AggregationContext context, int cellX, int cellY) {
        super(context, cellX, cellY);
    }

    @Override
    public double getCoverageUncertainty() {
        return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 5.0);
    }
}
