package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * Identity.
 *
 * @author Ralf Quast
 */
final class Identity implements Rule {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) {
        return sourceDescriptor;
    }

    @Override
    public Number apply(final Number number,
                        final VariableDescriptor sourceDescriptor) throws RuleException {
        return number;
    }
}
