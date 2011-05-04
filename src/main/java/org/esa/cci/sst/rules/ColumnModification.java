package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;

/**
 * Base class for rules that modify the column only.
 *
 * @author Ralf Quast
 */
abstract class ColumnModification implements Rule {

    @Override
    public final Number apply(Number number, Column sourceColumn) {
        return number;
    }
}
