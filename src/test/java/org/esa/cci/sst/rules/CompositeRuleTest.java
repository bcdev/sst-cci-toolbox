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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CompositeRuleTest {

    private static final Rule L = new AbstractAttributeModification() {

        @Override
        public Item apply(Item sourceColumn) {
            return new ColumnBuilder(sourceColumn).name("L").role("L").build();
        }
    };
    private static final Rule R = new AbstractAttributeModification() {

        @Override
        public Item apply(Item sourceColumn) {
            return new ColumnBuilder(sourceColumn).name("R").longName("R").build();
        }
    };

    private Item sourceColumn;

    @Before
    public void setUp() throws Exception {
        sourceColumn = new ColumnBuilder().build();
    }

    @Test
    public void testLeftAssociativeComposition() throws RuleException {
        final CompositeRule rule = new LeftAssociativeComposition(L, R);
        final Item targetColumn = rule.apply(sourceColumn);

        assertSame(L, rule.getRule(0));
        assertSame(R, rule.getRule(1));

        assertEquals("R", targetColumn.getName());
        assertEquals("R", targetColumn.getLongName());
        assertEquals("L", targetColumn.getRole());
    }

    @Test
    public void testRightAssociativeComposition() throws RuleException {
        final CompositeRule rule = new RightAssociativeComposition(L, R);
        final Item targetColumn = rule.apply(sourceColumn);

        assertSame(L, rule.getRule(0));
        assertSame(R, rule.getRule(1));

        assertEquals("L", targetColumn.getName());
        assertEquals("R", targetColumn.getLongName());
        assertEquals("L", targetColumn.getRole());
    }
}
