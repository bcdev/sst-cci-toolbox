/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
