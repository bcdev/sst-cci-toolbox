package org.esa.cci.sst.util;

/**
 * An abstract cell.
 *
 * @author Normasn Fomferra
 */
public abstract class AbstractCell implements Cell {
    private final int x;
    private final int y;

    protected AbstractCell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
