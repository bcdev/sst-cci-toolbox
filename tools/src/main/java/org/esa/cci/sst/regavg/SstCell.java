package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Cell;

import java.awt.*;

/**
 * A cell that can aggregate rectangular grid regions given by the {@link SstCellContext}.
 *
 * @author Norman Fomferra
 */
public abstract class SstCell implements Cell<SstCellContext> {

    @Override
    public abstract void aggregateSourceRect(SstCellContext sstCellContext, Rectangle rect);

    @Override
    public SstCell clone() {
        try {
            return (SstCell) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
