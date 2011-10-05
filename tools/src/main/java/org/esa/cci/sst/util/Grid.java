package org.esa.cci.sst.util;

/**
 * A grid.
 *
 * @author Norman Fomferra
 */
public interface Grid {
    GridDef getGridDef();

    double getSample(int x, int y);
}
