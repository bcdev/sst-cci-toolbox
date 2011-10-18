package org.esa.cci.sst.util;

/**
 * Creates cells.
 *
 * @author Norman Fomferra
 */
public interface CellFactory<C extends Cell> {
    C createCell();
}
