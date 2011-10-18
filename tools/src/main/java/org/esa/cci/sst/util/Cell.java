package org.esa.cci.sst.util;

import java.awt.*;

/**
 * A cell of a {@link CellGrid} used for numeric aggregations.
 *
 * @author Norman Fomferra
 */
public interface Cell<CT extends CellContext> extends Cloneable {

    boolean isEmpty();

    void aggregateSourceRect(CT cellContext, Rectangle rect);

    void accumulate(Cell<CT> cell);

    void accumulateAverage(Cell<CT> cell, double weight);

    Number[] getResults();

    Cell<CT> clone();
}
