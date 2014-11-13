package org.esa.cci.sst.common.file;

import org.esa.cci.sst.aggregate.AggregationContext;

final class Cell90 extends DefaultCellAggregationCell {

    Cell90(AggregationContext context, int cellX, int cellY) {
        super(context, cellX, cellY);
    }

    @Override
    public double getCoverageUncertainty() {
        final double uncertainty5 = super.getCoverageUncertainty();
        final double uncertainty90 = getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 90.0);
        return Math.sqrt(uncertainty5 * uncertainty5 + uncertainty90 * uncertainty90);
    }
}
