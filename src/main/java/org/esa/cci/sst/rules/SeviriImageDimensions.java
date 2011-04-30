package org.esa.cci.sst.rules;

/**
 * Replaces the second and third dimension with 'seviri.ni' and 'seviri.nj', respectively.
 */
final class SeviriImageDimensions extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(1, "seviri.ni");
        replacer.replace(2, "seviri.nj");
    }
}
