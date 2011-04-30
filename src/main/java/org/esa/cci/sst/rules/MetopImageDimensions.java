package org.esa.cci.sst.rules;

/**
 * Replaces the second and third dimension with 'metop.ni' and 'metop.nj', respectively.
 */
final class MetopImageDimensions extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(1, "metop.ni");
        replacer.replace(2, "metop.nj");
    }
}
