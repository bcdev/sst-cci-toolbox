package org.esa.cci.sst.rules;

/**
 * Replaces the first dimension with the 'matchup' dimension.
 *
 * @author Ralf Quast
 */
final class MatchupDimension extends DimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionReplacer replacer) throws RuleException {
        replacer.replace(0, "matchup");
    }
}
