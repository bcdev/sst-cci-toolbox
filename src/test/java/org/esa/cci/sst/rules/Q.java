package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

public class Q implements Rule {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        return null;
    }

    @Override
    public Number apply(Number number, VariableDescriptor sourceDescriptor) throws RuleException {
        return null;
    }
}
