package org.esa.cci.sst.rules;

/**
 * @author Ralf Quast
 */
final class RemoveXDimension extends AbstractDimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionStringBuilder builder) throws RuleException {
        builder.remove(2);
    }
}
