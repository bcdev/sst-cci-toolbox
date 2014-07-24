package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;

/**
 * @author Ralf Quast
 */
final class NoStandardName extends AbstractAttributeModification {

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.standardName(null);
    }
}
