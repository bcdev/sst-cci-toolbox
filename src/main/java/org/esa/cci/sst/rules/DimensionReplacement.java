package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Abstract base class for dimension replacement rules.
 *
 * @author Ralf Quast
 */
abstract class DimensionReplacement extends DescriptorModification {

    @Override
    public final VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        Assert.notNull(sourceDescriptor.getDimensions(), "dimensions");
        Assert.notEmpty(sourceDescriptor.getDimensions(), "dimensions");

        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        final String sourceDimensions = sourceDescriptor.getDimensions();
        final DimensionReplacer replacer = new DimensionReplacer(sourceDimensions);
        replaceDimensions(replacer);
        targetDescriptor.setDimensions(replacer.toString());

        return targetDescriptor;
    }

    protected abstract void replaceDimensions(DimensionReplacer replacer) throws RuleException;
}
