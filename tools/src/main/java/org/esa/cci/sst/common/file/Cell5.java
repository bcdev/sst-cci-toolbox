package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;

final class Cell5 extends DefaultSpatialAggregationCell {

    Cell5(AggregationContext context, int cellX, int cellY) {
        super(context, cellX, cellY);
    }

    @Override
    public double getCoverageUncertainty() {
        return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 5.0);
    }
}
