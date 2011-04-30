package org.esa.cci.sst.rules;

/**
 * Replaces the second and third dimension with 'amsre.ni' and 'amsre.nj', respectively.
 */
final class AmsreImageDimensions extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(1, "amsre.ni");
        replacer.replace(2, "amsre.nj");
    }
}
