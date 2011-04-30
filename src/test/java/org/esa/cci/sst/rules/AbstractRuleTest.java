package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic class for rule tests.
 *
 * @author Ralf Quast
 */
public abstract class AbstractRuleTest {

    private Rule rule;
    private VariableDescriptor sourceDescriptor;

    @Before
    public final void init() {
        rule = RuleFactory.getInstance().getRule(getClass().getSimpleName().replace("Test", ""));

        sourceDescriptor = new VariableDescriptor();
        configureSourceDescriptor();
    }

    @Test
    public final void testDescriptorConversion() throws RuleException {
        final Rule rule = getRule();
        final VariableDescriptor sourceDescriptor = getSourceDescriptor();
        final VariableDescriptor targetDescriptor = rule.apply(sourceDescriptor);

        assertTargetDescriptor(targetDescriptor);
    }

    @Test
    public abstract void testNumericConversion() throws RuleException;

    protected final Rule getRule() {
        return rule;
    }

    protected abstract void assertTargetDescriptor(VariableDescriptor targetDescriptor);

    protected abstract void configureSourceDescriptor();

    protected final VariableDescriptor getSourceDescriptor() {
        return sourceDescriptor;
    }

}
