package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.DescriptorBuilder;
import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Modifies the name of a descriptor.
 *
 * @author Ralf Quast
 */
final class Renaming extends DescriptorModification {

    private final String targetName;

    Renaming(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) {
        return new DescriptorBuilder(sourceDescriptor).setName(targetName).build();
    }
}
