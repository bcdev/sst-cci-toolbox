package org.esa.cci.sst.rules;

/**
 * Replaces the second and third dimension with 'seviri.ny' and 'seviri.nx', respectively.
 */
final class TmiImageDimensions extends AbstractDimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionStringBuilder builder) throws RuleException {
        builder.replace(1, "tmi.ny");
        builder.replace(2, "tmi.nx");
    }
}
