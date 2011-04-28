package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Replaces the second and third dimension with 'metop.ni' and 'metop.nj', respectively.
 */
public class MetopImageDimensions extends DescriptorModification {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        final String sourceDimensions = sourceDescriptor.getDimensions();
        String targetDimensions;
        targetDimensions = RuleUtil.replaceDimension(sourceDimensions, "metop.ni", 1);
        targetDimensions = RuleUtil.replaceDimension(targetDimensions, "metop.nj", 2);
        targetDescriptor.setDimensions(targetDimensions);
        return targetDescriptor;
    }
}
