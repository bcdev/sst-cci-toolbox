package org.esa.cci.sst;

import org.esa.cci.sst.grid.Grid;
import org.esa.cci.sst.grid.GridDef;
import org.junit.Ignore;

/**
 * @author Norman
 */
@Ignore
public class ScalarGrid implements Grid {
    private final GridDef gridDef;
    private final double scalar;

    public ScalarGrid(GridDef gridDef, double scalar) {
        this.gridDef = gridDef;
        this.scalar = scalar;
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public double getSampleDouble(int x, int y) {
        return scalar;
    }

    @Override
    public int getSampleInt(int x, int y) {
        return (int) scalar;
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        return scalar != 0;
    }
}
