package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.AbstractCell;

/**
 * A daily or monthly / 5º or 90º cell.
 *
 * @author Norman Fomferra
 */
public abstract class AbstractAggregationCell extends AbstractCell implements AggregationCell {
    private final CoverageUncertaintyProvider coverageUncertaintyProvider;

    protected AbstractAggregationCell(CoverageUncertaintyProvider coverageUncertaintyProvider, int x, int y) {
        super(x,y);
        this.coverageUncertaintyProvider = coverageUncertaintyProvider;
    }

    public final CoverageUncertaintyProvider getCoverageUncertaintyProvider() {
        return coverageUncertaintyProvider;
    }

    @Override
    public boolean isEmpty() {
        return getSampleCount() == 0;
    }
}
