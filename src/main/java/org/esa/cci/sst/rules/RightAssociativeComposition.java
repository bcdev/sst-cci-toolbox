package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Column;

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
    public Column apply(Column sourceColumn) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            sourceColumn = ruleList.get(i).apply(sourceColumn);
        }
        return sourceColumn;
    }

    @Override
    public Number apply(Number number, Column sourceColumn) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            final Rule rule = ruleList.get(i);
            number = rule.apply(number, sourceColumn);
            sourceColumn = rule.apply(sourceColumn);
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
