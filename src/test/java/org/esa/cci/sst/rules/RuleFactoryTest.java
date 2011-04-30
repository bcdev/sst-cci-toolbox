package org.esa.cci.sst.rules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuleFactoryTest {

    @Test
    public void testGetSimpleRule() {
        final Rule rule = RuleFactory.getInstance().getRule("P");

        assertTrue(rule instanceof P);
    }

    @Test
    public void testGetSimpleRuleWithImplicitRenaming() {
        final Rule rule = RuleFactory.getInstance().getRule("P", "R");

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Renaming);
        assertTrue(composition.getRule(1) instanceof P);
    }

    @Test
    public void testGetRuleComposite() {
        final Rule rule = RuleFactory.getInstance().getRule("Q,P");

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Q);
        assertTrue(composition.getRule(1) instanceof P);
    }

    @Test
    public void testGetRuleCompositeWithImplicitRenaming() {
        final Rule rule = RuleFactory.getInstance().getRule("Q,P", "R");

        assertTrue(rule instanceof RightAssociativeComposition);

        final RightAssociativeComposition composition = (RightAssociativeComposition) rule;

        assertEquals(2, composition.getRuleCount());
        assertTrue(composition.getRule(0) instanceof Renaming);
        assertTrue(composition.getRule(1) instanceof RightAssociativeComposition);
    }

    @Test
    public void testGetRenamingRule() {
        final Rule rule = RuleFactory.getInstance().getRenamingRule("R");

        assertTrue(rule instanceof Renaming);
    }


}
