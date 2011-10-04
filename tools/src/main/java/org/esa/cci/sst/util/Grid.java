package org.esa.cci.sst.util;

// todo - make it applicable to multiple variables

/**
 * A grid is a sparsely filled, rectangular arrangement of {@link GridCell}s.
 *
 * @author Norman Fomferra
 */
public class Grid {
    private final GridDef gridDef;
    private final GridCell[][] gridCells;

    public Grid(GridDef gridDef) {
        this.gridDef = gridDef;
        gridCells = new GridCell[gridDef.getHeight()][gridDef.getWidth()];
    }

    public GridDef getGridDef() {
        return gridDef;
    }

    public GridCell getGridCellSafe(int cellX, int cellY) {
        GridCell gridCell = getGridCell(cellX, cellY);
        if (gridCell == null) {
            gridCell = createGridCell();
            setGridCell(cellX, cellY, gridCell);
        }
        return gridCell;
    }

    public GridCell getGridCell(int cellX, int cellY) {
        return gridCells[cellY][cellX];
    }

    public void setGridCell(int cellX, int cellY, GridCell gridCell) {
        gridCells[cellY][cellX] = gridCell;
    }

    private GridCell createGridCell() {
        return new GridCell();
    }

    public GridCell combine() {
        GridCell combinedGridCell = createGridCell();
        for (int cellY = 0; cellY < gridDef.getHeight(); cellY++) {
            for (int cellX = 0; cellX < gridDef.getWidth(); cellX++) {
                GridCell gridCell = gridCells[cellY][cellX];
                if (gridCell != null) {
                    combinedGridCell.aggregate(gridCell);
                }
            }
        }
        return combinedGridCell;
    }

}
