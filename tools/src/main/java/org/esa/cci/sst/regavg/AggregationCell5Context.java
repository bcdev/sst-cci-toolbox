package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;

/**
 * Provides the input grids for an {@link AggregationCell5}.
 *
 * @author Norman Fomferra
*/
public final class AggregationCell5Context {
    private final GridDef sourceGridDef;
    private final Grid[] sourceGrids;
    private final Grid analysedSstGrid;
    private final Grid seaCoverageGrid;

    public AggregationCell5Context(GridDef sourceGridDef, Grid[] sourceGrids, Grid analysedSstGrid, Grid seaCoverageGrid) {
        this.sourceGridDef = sourceGridDef;
        this.sourceGrids = sourceGrids;
        this.analysedSstGrid = analysedSstGrid;
        this.seaCoverageGrid = seaCoverageGrid;
    }

    public GridDef getSourceGridDef() {
        return sourceGridDef;
    }

    public Grid[] getSourceGrids() {
        return sourceGrids;
    }

    public Grid getAnalysedSstGrid() {
        return analysedSstGrid;
    }

    public Grid getSeaCoverageGrid() {
        return seaCoverageGrid;
    }
}
