package org.esa.cci.sst.rules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AssertTest {

    @Test
    public void testCondition_pass() throws RuleException {
        Assert.condition(true, "don't care");
    }

    @Test
    public void testCondition_fail() throws RuleException {
        try {
            Assert.condition(false, "must fail");
            fail("RuleException expected");
        } catch (RuleException expected) {
            assertEquals("Expected condition 'must fail' to be satisfied.", expected.getMessage());
        }
    }
}
