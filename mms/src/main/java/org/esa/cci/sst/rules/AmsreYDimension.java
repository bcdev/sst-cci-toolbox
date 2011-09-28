package org.esa.cci.sst.rules;

/**
 * Replaces the second dimension with 'amsre.ny'.
 *
 * @author Ralf Quast
 */
final class AmsreYDimension extends AbstractDimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionStringBuilder builder) throws RuleException {
        builder.replace(1, "amsre.ny");
    }
}