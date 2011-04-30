package org.esa.cci.sst.rules;

/**
 * Replaces the second dimension with the 'filename.length' dimension.
 *
 * @author Ralf Quast
 */
final class FilenameDimension extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(1, "filename.length");
    }
}
