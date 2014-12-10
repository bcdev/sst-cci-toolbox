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

package org.esa.cci.sst.grid;

/**
 * Grid decorator for permuting sample values. Is used for flipping sample
 * values in y-direction or transposing the sample values for the left and
 * right halves of a grid.
 *
 * @author Ralf Quast
 */
public abstract class AbstractSamplePermuter implements Grid {

    private final Grid grid;

    public AbstractSamplePermuter(Grid grid) {
        this.grid = grid;
    }

    @Override
    public final GridDef getGridDef() {
        return grid.getGridDef();
    }

    @Override
    public final double getSampleDouble(int x, int y) {
        return grid.getSampleDouble(getSourceX(x), getSourceY(y));
    }

    @Override
    public final int getSampleInt(int x, int y) {
        return grid.getSampleInt(getSourceX(x), getSourceY(y));
    }

    @Override
    public final boolean getSampleBoolean(int x, int y) {
        return grid.getSampleBoolean(getSourceX(x), getSourceY(y));
    }

    protected abstract int getSourceX(int x);

    protected abstract int getSourceY(int y);
}
