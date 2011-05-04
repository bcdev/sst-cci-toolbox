package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;

/**
 * Identity.
 *
 * @author Ralf Quast
 */
final class Identity implements Rule {

    @Override
    public Column apply(Column sourceColumn) {
        return sourceColumn;
    }

    @Override
    public Number apply(Number number, Column sourceColumn) {
        return number;
    }
}
