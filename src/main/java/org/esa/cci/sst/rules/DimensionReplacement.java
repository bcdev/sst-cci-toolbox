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
        final String sourceDimensions = sourceDescriptor.getDimensions();
        Assert.notNull(sourceDimensions, "dimensions");
        Assert.notEmpty(sourceDimensions, "dimensions");

        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        final DimensionReplacer replacer = new DimensionReplacer(sourceDimensions);
        replaceDimensions(replacer);

        targetDescriptor.setDimensions(replacer.toString());

        return targetDescriptor;
    }

    protected abstract void replaceDimensions(DimensionReplacer replacer) throws RuleException;
}
