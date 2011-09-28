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

import org.junit.Test;

import static org.junit.Assert.*;

public class RuleFactoryTest {

    @Test
    public void testRuleSpecifications() {
        final RuleFactory factory = RuleFactory.getInstance();

        assertNotNull(factory.getRule("P"));
        assertNotNull(factory.getRule("P "));
        assertNotNull(factory.getRule(" P"));
        assertNotNull(factory.getRule("P,Q"));
        assertNotNull(factory.getRule("P, Q"));
        assertNotNull(factory.getRule("P,Q "));
        assertNotNull(factory.getRule("P,,Q"));

        try {
            factory.getRule("P,");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            factory.getRule(",P");
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetSimpleRule() {
        final Rule rule = RuleFactory.getInstance().getRule("P");

        assertTrue(rule instanceof P);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSimpleRule_Unknown() {
        RuleFactory.getInstance().getRule("U");
    }

    @Test
    public void testGetSimpleRule_WithImplicitRenaming() {
        final Rule rule = RuleFactory.getInstance().getRule("P", "R");

        assertTrue(rule instanceof LeftAssociativeComposition);

        final CompositeRule composition = (CompositeRule) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof P);
        assertTrue(composition.getRule(1) instanceof Renaming);
    }

    @Test
    public void testGetRuleComposite() {
        final Rule rule = RuleFactory.getInstance().getRule("Q, P");

        assertTrue(rule instanceof LeftAssociativeComposition);

        final CompositeRule composition = (CompositeRule) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Q);
        assertTrue(composition.getRule(1) instanceof P);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRuleComposite_Unknown() {
        RuleFactory.getInstance().getRule("U,P");
    }

    @Test
    public void testGetRuleComposite_WithImplicitRenaming() {
        final Rule rule = RuleFactory.getInstance().getRule("Q,P", "R");

        assertTrue(rule instanceof LeftAssociativeComposition);

        final CompositeRule composition = (CompositeRule) rule;

        assertEquals(3, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Q);
        assertTrue(composition.getRule(1) instanceof P);
        assertTrue(composition.getRule(2) instanceof Renaming);
    }

    @Test
    public void testGetRenamingRule() {
        final Rule rule = RuleFactory.getInstance().getRenamingRule("R");

        assertTrue(rule instanceof Renaming);
    }
}
