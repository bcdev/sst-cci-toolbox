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

import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A right-associative composition of rules.
 *
 * @author Ralf Quast
 */
final class RightAssociativeComposition extends CompositeRule {

    private final List<Rule> ruleList = new ArrayList<Rule>();

    RightAssociativeComposition(Rule... rules) {
        Collections.addAll(ruleList, rules);
    }

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        for (int i = ruleList.size(); i-- > 0;) {
            sourceColumn = ruleList.get(i).apply(sourceColumn);
        }
        return sourceColumn;
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        // todo - optimize computation by skipping non-numeric rules
        for (int i = ruleList.size(); i-- > 0;) {
            final Rule rule = ruleList.get(i);
            rule.setContext(getContext());
            sourceArray = rule.apply(sourceArray, sourceColumn);
            sourceColumn = rule.apply(sourceColumn);
        }
        return sourceArray;
    }

    @Override
    public RightAssociativeComposition append(Rule rule) {
        ruleList.add(0, rule);
        return this;
    }

    @Override
    public Rule getRule(int i) {
        return ruleList.get(i);
    }

    @Override
    public int getRuleCount() {
        return ruleList.size();
    }
}
