package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Replaces the first dimension with the 'matchup' dimension.
 *
 * @author Ralf Quast
 */
final class MatchupDimension extends DescriptorModification {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        final String sourceDimensions = sourceDescriptor.getDimensions();
        final String targetDimensions = RuleUtil.replaceDimension(sourceDimensions, "matchup", 0);
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setDimensions(targetDimensions);

        return targetDescriptor;
    }
}
