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

package org.esa.cci.sst.common.cellgrid;

import org.esa.cci.sst.common.GridDef;
import org.esa.cci.sst.common.cell.AbstractCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Norman
 */
public class CellGridTest {

    @Test
    public void testSize() throws Exception {
        CellGrid<MyCell> grid = createGrid(GridDef.createGlobal(5.0));

        assertEquals(72, grid.getWidth());
        assertEquals(36, grid.getHeight());
    }

    @Test
    public void testGetCellSafe() throws Exception {
        CellGrid<MyCell> grid = createGrid(GridDef.createGlobal(5.0));

        assertNull(grid.getCell(0, 9));
        MyCell cell = grid.getCellSafe(0, 9);
        assertNotNull(cell);
        assertSame(cell, grid.getCell(0, 9));
        assertSame(cell, grid.getCellSafe(0, 9));
    }

    @Test
    public void testSetCell() throws Exception {
        CellGrid<MyCell> grid = createGrid(GridDef.createGlobal(5.0));

        assertNull(grid.getCell(6, 18));
        MyCell cell = new MyCell(6, 18);
        grid.setCell(6, 18, cell);
        assertSame(cell, grid.getCell(6, 18));
        assertSame(cell, grid.getCellSafe(6, 18));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetCellIAE() throws Exception {
        CellGrid<MyCell> grid = createGrid(GridDef.createGlobal(5.0));

        MyCell cell = new MyCell(7, 18);
        grid.setCell(6, 18, cell);
    }

    @Test
    public void testGetCells() throws Exception {
        CellGrid<MyCell> grid = createGrid(GridDef.createGlobal(5.0));

        List<MyCell> cells = grid.getCells();
        assertNotNull(cells);
        assertEquals(0, cells.size());

        grid.getCellSafe(0, 9);
        grid.getCellSafe(10, 2);
        grid.getCellSafe(12, 25);
        grid.getCellSafe(7, 3);

        cells = grid.getCells();
        assertNotNull(cells);
        assertEquals(4, cells.size());
        assertSame(grid.getCell(10, 2), cells.get(0));
        assertSame(grid.getCell(7, 3), cells.get(1));
        assertSame(grid.getCell(0, 9), cells.get(2));
        assertSame(grid.getCell(12, 25), cells.get(3));
    }

    private CellGrid<MyCell> createGrid(GridDef gridDef) {
        return CellGrid.create(gridDef, new CellFactory<MyCell>() {
            @Override
            public MyCell createCell(int cellX, int cellY) {
                return new MyCell(cellX, cellY);
            }
        });
    }

    private static class MyCell extends AbstractCell {

        boolean empty;

        private MyCell(int x, int y) {
            super(x, y);
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }
    }

    @Test
    public void testName() throws Exception {

    }
}
