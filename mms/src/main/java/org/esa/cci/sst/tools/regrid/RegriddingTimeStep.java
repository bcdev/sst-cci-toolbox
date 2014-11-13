/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.common.TimeStep;
import org.esa.cci.sst.common.AggregationCell;
import org.esa.cci.sst.common.CellGrid;

import java.util.Date;

final class RegriddingTimeStep implements TimeStep {

    private final Date startDate;
    private final Date endDate;
    private final CellGrid<? extends AggregationCell> cellGrid;

    RegriddingTimeStep(Date startDate, Date endDate, CellGrid<? extends AggregationCell> cellGrid) {
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
