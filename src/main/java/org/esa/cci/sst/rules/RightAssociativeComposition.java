package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A right-associative composition of rules.
 *
 * @author Ralf Quast
 */
final class RightAssociativeComposition implements Rule {

    private final List<Rule> ruleList = new ArrayList<Rule>();

    RightAssociativeComposition(Rule... rules) {
        Collections.addAll(ruleList, rules);
    }

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            sourceDescriptor = ruleList.get(i).apply(sourceDescriptor);
        }
        return sourceDescriptor;
    }

    @Override
    public Number apply(Number number, VariableDescriptor sourceDescriptor) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            final Rule rule = ruleList.get(i);
            number = rule.apply(number, sourceDescriptor);
            sourceDescriptor = rule.apply(sourceDescriptor);
        }
        return number;
    }

    RightAssociativeComposition prepend(Rule rule) {
        ruleList.add(0, rule);
        return this;
    }

    Rule getRule(int i) {
        return ruleList.get(i);
    }

    int getRuleCount() {
        return ruleList.size();
    }
}
