package org.esa.cci.sst.rules;

/**
 * Replaces the second dimension with 'tmi.ny'.
 *
 * @author Ralf Quast
 */
final class TmiYDimension extends AbstractDimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionStringBuilder builder) throws RuleException {
        builder.replace(1, "tmi.ny");
    }
}