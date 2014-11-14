package org.esa.cci.sst.grid;/*
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

/**
 * Decorator for creating a mask from an existing grid.
 *
 * @author Ralf Quast
 */
public final class Mask implements Grid {

    private final Grid sourceGrid;
    private final int mask;

    private Mask(Grid sourceGrid, int mask) {
        this.sourceGrid = sourceGrid;
        this.mask = mask;
    }

    public static Grid create(Grid sourceGrid, int mask) {
        return new Mask(sourceGrid, mask);
    }

    @Override
    public GridDef getGridDef() {
        return sourceGrid.getGridDef();
    }

    @Override
    public double getSampleDouble(int x, int y) {
        return getSampleInt(x, y);
    }

    @Override
    public int getSampleInt(int x, int y) {
        return (sourceGrid.getSampleInt(x, y) & mask) == mask ? 1 : 0;
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        return getSampleInt(x, y) != 0;
    }
}
