package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;

/**
 * Identity.
 *
 * @author Ralf Quast
 */
final class Identity implements Rule {

    @Override
    public Descriptor apply(Descriptor sourceDescriptor) {
        return sourceDescriptor;
    }

    @Override
    public Number apply(Number number, Descriptor sourceDescriptor) {
        return number;
    }
}
