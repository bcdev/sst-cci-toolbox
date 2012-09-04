package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.TimeStep;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;

import java.util.Date;

/**
 * {@author Bettina Scholze}
 * Date: 13.08.12 16:12
 */
public class RegriddingTimeStep implements TimeStep {

    private final Date startDate;
    private final Date endDate;
    private final CellGrid<? extends AggregationCell> cellGrid;

    public RegriddingTimeStep(Date startDate, Date endDate, CellGrid<? extends AggregationCell> cellGrid) {
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

    public CellGrid<? extends AggregationCell> getCellGrid() {
        return cellGrid;
    }
}
