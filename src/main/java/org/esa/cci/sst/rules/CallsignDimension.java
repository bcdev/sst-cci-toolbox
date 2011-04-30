package org.esa.cci.sst.rules;

/**
 * Replaces the second dimension with the 'callsign.length' dimension.
 *
 * @author Ralf Quast
 */
final class CallsignDimension extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(1, "callsign.length");
    }
}
