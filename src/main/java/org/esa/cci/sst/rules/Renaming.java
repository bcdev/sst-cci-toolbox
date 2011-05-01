package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
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
    public Descriptor apply(Descriptor sourceDescriptor) {
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setName(targetName);

        return targetDescriptor;
    }
}
