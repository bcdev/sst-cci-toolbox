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

package org.esa.cci.sst.common.cell;

import org.esa.cci.sst.common.CellAccumulator;

/**
 * A cell that aggregates SpatialAggregationCells.
 * <p>
 * E.g. for the RegionalAveraging Tool: A daily or monthly / 90º cell that accumulates daily or monthly / 5º cells ({@link org.esa.cci.sst.common.cell.SpatialAggregationCell}).
 * </p>
 * @author Norman Fomferra
 */
public interface CellAggregationCell<C extends AggregationCell> extends AggregationCell, CellAccumulator<C> {

    @Override
    void accumulate(C cell, double weight);
}
