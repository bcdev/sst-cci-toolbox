/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnI;

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
    public ColumnI apply(ColumnI sourceColumn) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            sourceColumn = ruleList.get(i).apply(sourceColumn);
        }
        return sourceColumn;
    }

    @Override
    public Number apply(Number number, ColumnI sourceColumn) throws RuleException {
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
