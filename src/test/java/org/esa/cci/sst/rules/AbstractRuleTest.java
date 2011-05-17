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

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic class for rule tests.
 *
 * @author Ralf Quast
 */
public abstract class AbstractRuleTest {

    private Rule rule;
    private Item sourceColumn;

    @Before
    public final void setUp() {
        final RuleFactory ruleFactory = RuleFactory.getInstance();
        rule = ruleFactory.getRule(getClass().getSimpleName().replace("Test", ""));
        sourceColumn = configureSourceColumn(new ColumnBuilder()).build();
    }

    @Test
    public final void testColumnConversion() throws RuleException {
        final Rule rule = getRule();
        final Item sourceColumn = getSourceColumn();
        final Item targetColumn = rule.apply(sourceColumn);

        assertTargetColumn(targetColumn);
    }

    protected abstract ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder);

    protected abstract void assertTargetColumn(Item targetColumn);

    @Test
    public abstract void testNumericConversion() throws RuleException;

    protected final Rule getRule() {
        return rule;
    }

    protected final Item getSourceColumn() {
        return sourceColumn;
    }

}
