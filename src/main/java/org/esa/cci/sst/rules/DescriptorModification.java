package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Base class for rules that modify the descriptor only.
 *
 * @author Ralf Quast
 */
abstract class DescriptorModification implements Rule {

    @Override
    public final Number apply(Number number, final VariableDescriptor targetDescriptor,
                              final VariableDescriptor sourceDescriptor) {
        return number;
    }
}
