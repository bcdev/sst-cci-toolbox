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

package org.esa.cci.sst.util;

/**
 * A sparsely filled, regular, rectangular arrangement of {@link Cell}s used for numeric aggregations.
 *
 * @author Norman Fomferra
 */
public class CellGrid<C extends Cell> {
    private final GridDef gridDef;
    private final CellFactory<C> cellFactory;
    private final Cell[][] cells;

    public CellGrid(GridDef gridDef, CellFactory<C> cellFactory) {
        this.gridDef = gridDef;
        this.cellFactory = cellFactory;
        cells = new Cell[gridDef.getHeight()][gridDef.getWidth()];
    }

    public GridDef getGridDef() {
        return gridDef;
    }

    public CellFactory<C> getCellFactory() {
        return cellFactory;
    }

    public C getCellSafe(int cellX, int cellY) {
        C cell = getCell(cellX, cellY);
        if (cell == null) {
            cell = createCell(cellX, cellY);
            setCell(cellX, cellY, cell);
        }
        return cell;
    }

    public C getCell(int cellX, int cellY) {
        //noinspection unchecked
        return (C) cells[cellY][cellX];
    }

    public void setCell(int cellX, int cellY, C cell) {
        cells[cellY][cellX] = cell;
    }

    public C createCell(int cellX, int cellY) {
        return getCellFactory().createCell(cellX, cellY);
    }

}
