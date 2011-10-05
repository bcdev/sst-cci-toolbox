package org.esa.cci.sst.util;

/**
 * A grid.
 *
 * @author Norman Fomferra
 */
public interface Grid {
    GridDef getGridDef();

    double getSampleDouble(int x, int y);

    int getSampleInt(int x, int y);

    boolean getSampleBoolean(int x, int y);
}
