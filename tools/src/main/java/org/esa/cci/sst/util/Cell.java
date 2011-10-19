package org.esa.cci.sst.util;

/**
 * A cell of a {@link CellGrid} used for numeric aggregations.
 *
 * @author Norman Fomferra
 */
public interface Cell {

    boolean isEmpty();

    int getX();

    int getY();
}
