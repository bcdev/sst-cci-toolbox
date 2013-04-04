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

package org.esa.cci.sst.common;

import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;

/**
 * Provides the input grids for a {@link org.esa.cci.sst.common.cell.SpatialAggregationCell}.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public final class SpatialAggregationContext {
    private final GridDef sourceGridDef;
    private final Grid[] sourceGrids;
    private final Grid climatologySst; //in source resolution
    private final Grid climatologySeaCoverage; //in source resolution

    private Grid standardDeviation; //in source resolution


    public SpatialAggregationContext(GridDef sourceGridDef, Grid[] sourceGrids, Grid climatologySst, Grid climatologySeaCoverage) {
        this.sourceGridDef = sourceGridDef;
        this.sourceGrids = sourceGrids;
        this.climatologySst = climatologySst;
        this.climatologySeaCoverage = climatologySeaCoverage;
    }

    public GridDef getSourceGridDef() {
        return sourceGridDef;
    }

    public Grid[] getSourceGrids() {
        return sourceGrids;
    }

    public Grid getClimatologySst() {
        return climatologySst;
    }

    public Grid getClimatologySeaCoverage() {
        return climatologySeaCoverage;
    }

    public Grid getStandardDeviation() {
        return standardDeviation;
    }

    public SpatialAggregationContext setStandardDeviation(Grid standardDeviation) {
        this.standardDeviation = standardDeviation;
        return this;
    }
}
