package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.DescriptorBuilder;

/**
 * Abstract base class for dimension replacement rules.
 *
 * @author Ralf Quast
 */
abstract class DimensionReplacement extends DescriptorModification {

    @Override
    public final Descriptor apply(Descriptor sourceDescriptor) throws RuleException {
        final String sourceDimensions = sourceDescriptor.getDimensions();
        Assert.notNull(sourceDimensions, "dimensions");
        Assert.notEmpty(sourceDimensions, "dimensions");

        final DescriptorBuilder builder = new DescriptorBuilder(sourceDescriptor);
        final DimensionReplacer replacer = new DimensionReplacer(sourceDimensions);

        replaceDimensions(replacer);
        builder.setDimensions(replacer.toString());

        return builder.build();
    }

    protected abstract void replaceDimensions(DimensionReplacer replacer) throws RuleException;
}
