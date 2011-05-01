package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;

final class Q implements Rule {

    @Override
    public Descriptor apply(Descriptor sourceDescriptor) throws RuleException {
        return null;
    }

    @Override
    public Number apply(Number number, Descriptor sourceDescriptor) throws RuleException {
        return null;
    }
}
