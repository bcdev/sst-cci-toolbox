package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Replaces the second dimension with the 'filename.length' dimension.
 *
 * @author Ralf Quast
 */
class FilenameDimension extends DescriptorModification {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        final String sourceDimensions = sourceDescriptor.getDimensions();
        final String targetDimensions = RuleUtil.replaceDimension(sourceDimensions, "filename.length", 1);
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setDimensions(targetDimensions);

        return targetDescriptor;
    }

}
