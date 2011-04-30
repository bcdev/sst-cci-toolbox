package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Replaces the second and third dimension with 'amsre.ni' and 'amsre.nj', respectively.
 */
class AmsreImageDimensions extends DescriptorModification {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        final String sourceDimensions = sourceDescriptor.getDimensions();
        String targetDimensions;
        targetDimensions = RuleUtil.replaceDimension(sourceDimensions, "amsre.ni", 1);
        targetDimensions = RuleUtil.replaceDimension(targetDimensions, "amsre.nj", 2);
        targetDescriptor.setDimensions(targetDimensions);
        return targetDescriptor;
    }
}
