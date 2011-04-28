package org.esa.cci.sst.rules;

/**
 * Base class for rules that modify the descriptor only.
 *
 * @author Ralf Quast
 */
abstract class DescriptorModification implements Rule {

    @Override
    public final Number apply(Number number) {
        return number;
    }
}
