package org.esa.cci.sst.util;

// todo - make it applicable to multiple variables

/**
 * A sparsely filled, regular, rectangular arrangement of {@link Cell}s used for averaging.
 *
 * @author Norman Fomferra
 */
public class CellGrid implements Grid {
    private final GridDef gridDef;
    private final Cell[][] cells;

    public CellGrid(GridDef gridDef) {
        this.gridDef = gridDef;
        cells = new Cell[gridDef.getHeight()][gridDef.getWidth()];
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public double getSample(int x, int y) {
        Cell cell = getCell(x, y);
        if (cell == null) {
            return Double.NaN;
        }
        return cell.getMean();
    }

    public Cell getCellSafe(int cellX, int cellY) {
        Cell cell = getCell(cellX, cellY);
        if (cell == null) {
            cell = createCell();
            setCell(cellX, cellY, cell);
        }
        return cell;
    }


    public Cell getCell(int cellX, int cellY) {
        return cells[cellY][cellX];
    }

    public void setCell(int cellX, int cellY, Cell cell) {
        cells[cellY][cellX] = cell;
    }

    private Cell createCell() {
        return new Cell();
    }

    public Cell combine() {
        Cell combinedCell = createCell();
        for (int cellY = 0; cellY < gridDef.getHeight(); cellY++) {
            for (int cellX = 0; cellX < gridDef.getWidth(); cellX++) {
                Cell cell = cells[cellY][cellX];
                if (cell != null) {
                    combinedCell.accumulate(cell);
                }
            }
        }
        return combinedCell;
    }

}
