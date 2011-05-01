package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;

/**
 * Base class for rules that modify the descriptor only.
 *
 * @author Ralf Quast
 */
abstract class DescriptorModification implements Rule {

    @Override
    public final Number apply(Number number, Descriptor sourceDescriptor) {
        return number;
    }
}
