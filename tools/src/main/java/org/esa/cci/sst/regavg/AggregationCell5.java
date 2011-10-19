package org.esa.cci.sst.regavg;

import java.awt.*;

/**
 * A daily or monthly / 5º cell that aggregates rectangular regions of source grids.
 *
 * @author Norman Fomferra
 */
public interface AggregationCell5 extends AggregationCell {
    void accumulate(AggregationCell5Context aggregationCell5Context, Rectangle rect);
}
