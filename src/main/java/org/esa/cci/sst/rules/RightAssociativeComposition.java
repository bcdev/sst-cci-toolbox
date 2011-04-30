package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

/**
 * A right-associative composition of rules.
 *
 * @author Ralf Quast
 */
final class RightAssociativeComposition implements Rule {

    private final Rule[] rules;

    RightAssociativeComposition(Rule... rules) {
        this.rules = rules;
    }

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        for (int i = rules.length; i-- > 0;) {
            sourceDescriptor = rules[i].apply(sourceDescriptor);
        }
        return sourceDescriptor;
    }

    @Override
    public Number apply(Number number, VariableDescriptor sourceDescriptor) throws RuleException {
        for (int i = rules.length; i-- > 0;) {
            number = rules[i].apply(number, null);
        }
        return number;
    }

    Rule getRule(int i) {
        return rules[i];
    }

    int getRuleCount() {
        return rules.length;
    }
}
