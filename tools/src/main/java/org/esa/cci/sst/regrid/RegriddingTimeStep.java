package org.esa.cci.sst.regrid;

import org.esa.cci.sst.regavg.AggregationCell5;
import org.esa.cci.sst.util.CellGrid;
import org.esa.cci.sst.util.TimeStep;

import java.util.Date;

/**
 * {@author Bettina Scholze}
 * Date: 13.08.12 16:12
 */
public class RegriddingTimeStep  implements TimeStep {

    private final Date startDate;
    private final Date endDate;
    private final CellGrid<AggregationCell5> cellGrid;

    public RegriddingTimeStep(Date startDate, Date endDate, CellGrid<AggregationCell5> cellGrid) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.cellGrid = cellGrid;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public CellGrid<AggregationCell5> getCellGrid() {
        return cellGrid;
    }
}
