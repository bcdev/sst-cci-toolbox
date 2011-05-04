package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;

/**
 * Modifies the name of a column.
 *
 * @author Ralf Quast
 */
final class Renaming extends ColumnModification {

    private final String targetName;

    Renaming(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public Column apply(Column sourceColumn) {
        return new ColumnBuilder(sourceColumn).setName(targetName).build();
    }
}
