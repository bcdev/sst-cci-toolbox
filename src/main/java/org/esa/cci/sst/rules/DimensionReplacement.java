package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;

/**
 * Abstract base class for dimension replacement rules.
 *
 * @author Ralf Quast
 */
abstract class DimensionReplacement extends ColumnModification {

    @Override
    public final Column apply(Column sourceColumn) throws RuleException {
        final String sourceDimensions = sourceColumn.getDimensions();
        Assert.notNull(sourceDimensions, "dimensions");
        Assert.notEmpty(sourceDimensions, "dimensions");

        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        final DimensionReplacer replacer = new DimensionReplacer(sourceDimensions);

        replaceDimensions(replacer);
        builder.setDimensions(replacer.toString());

        return builder.build();
    }

    protected abstract void replaceDimensions(DimensionReplacer replacer) throws RuleException;
}
