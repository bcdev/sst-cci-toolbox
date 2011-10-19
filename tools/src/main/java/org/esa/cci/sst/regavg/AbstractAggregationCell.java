package org.esa.cci.sst.regavg;

/**
 * A daily or monthly / 5º or 90º cell.
 *
 * @author Norman Fomferra
 */
public abstract class AbstractAggregationCell implements AggregationCell {
    private final CoverageUncertaintyProvider coverageUncertaintyProvider;
    private final int x;
    private final int y;

    protected AbstractAggregationCell(CoverageUncertaintyProvider coverageUncertaintyProvider, int x, int y) {
        this.coverageUncertaintyProvider = coverageUncertaintyProvider;
        this.x = x;
        this.y = y;
    }

    public final CoverageUncertaintyProvider getCoverageUncertaintyProvider() {
        return coverageUncertaintyProvider;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public boolean isEmpty() {
        return getSampleCount() == 0;
    }

}
