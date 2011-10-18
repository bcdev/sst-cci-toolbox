package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.CellContext;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;

/**
 * Represents an SST averaging context that is used by {@link SstCell}s.
 *
 * @author Norman Fomferra
*/
public class SstCellContext implements CellContext {
    final GridDef sourceGridDef;
    final Grid[] sourceGrids;
    final Grid analysedSstGrid;
    final Grid seaCoverageGrid;

    SstCellContext(GridDef sourceGridDef, Grid[] sourceGrids, Grid analysedSstGrid, Grid seaCoverageGrid) {
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
