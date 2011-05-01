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

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Renaming);
        assertTrue(composition.getRule(1) instanceof P);
    }

    @Test
    public void testGetRuleComposite() {
        final Rule rule = RuleFactory.getInstance().getRule("Q, P");

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

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

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

        assertEquals(3, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Renaming);
        assertTrue(composition.getRule(1) instanceof Q);
        assertTrue(composition.getRule(2) instanceof P);
    }

    @Test
    public void testGetRenamingRule() {
        final Rule rule = RuleFactory.getInstance().getRenamingRule("R");

        assertTrue(rule instanceof Renaming);
    }
}
