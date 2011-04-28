package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Replaces the second dimension with the 'callsign.length' dimension.
 *
 * @author Ralf Quast
 */
class CallsignDimension extends DescriptorModification {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) {
        final String sourceDimensions = sourceDescriptor.getDimensions();
        final String targetDimensions = RuleUtil.replaceDimension(sourceDimensions, "callsign.length", 1);
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setDimensions(targetDimensions);

        return targetDescriptor;
    }

}
