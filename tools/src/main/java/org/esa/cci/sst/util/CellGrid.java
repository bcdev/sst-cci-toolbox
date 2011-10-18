package org.esa.cci.sst.util;

/**
 * A sparsely filled, regular, rectangular arrangement of {@link Cell}s used for numeric aggregations.
 *
 * @author Norman Fomferra
 */
public class CellGrid<CT extends CellContext, C extends Cell<CT>> {
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
            cell = createCell();
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

    public C createCell() {
        return getCellFactory().createCell();
    }

    public C aggregate(Grid weightGrid) {
        C combinedCell = createCell();
        for (int cellY = 0; cellY < gridDef.getHeight(); cellY++) {
            for (int cellX = 0; cellX < gridDef.getWidth(); cellX++) {
                C cell = getCell(cellX, cellY);
                if (cell != null) {
                    combinedCell.accumulateAverage(cell, weightGrid.getSampleDouble(cellX, cellY));
                }
            }
        }
        return combinedCell;
    }

}
