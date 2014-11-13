package org.esa.cci.sst.common.file;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import org.esa.cci.sst.aggregate.AggregationContext;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.aggregate.SpatialAggregationCell;

public class SingleDayAggregationCellFactory implements CellFactory<SpatialAggregationCell> {

    private final AggregationContext context;

    SingleDayAggregationCellFactory(AggregationContext context) {
        this.context = context;
    }

    @Override
    public SpatialAggregationCell createCell(int cellX, int cellY) {
        return new SingleDayAggregationCell(context, cellX, cellY);
    }
}
