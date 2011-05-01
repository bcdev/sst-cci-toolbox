package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.DescriptorBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic class for rule tests.
 *
 * @author Ralf Quast
 */
public abstract class AbstractRuleTest {

    private Rule rule;
    private Descriptor sourceDescriptor;

    @Before
    public final void init() {
        rule = RuleFactory.getInstance().getRule(getClass().getSimpleName().replace("Test", ""));
        sourceDescriptor = configureSourceDescriptorBuilder(new DescriptorBuilder()).build();
    }

    @Test
    public final void testDescriptorConversion() throws RuleException {
        final Rule rule = getRule();
        final Descriptor sourceDescriptor = getSourceDescriptor();
        final Descriptor targetDescriptor = rule.apply(sourceDescriptor);

        assertTargetDescriptor(targetDescriptor);
    }

    @Test
    public abstract void testNumericConversion() throws RuleException;

    protected final Rule getRule() {
        return rule;
    }

    protected abstract void assertTargetDescriptor(Descriptor targetDescriptor);

    protected abstract DescriptorBuilder configureSourceDescriptorBuilder(DescriptorBuilder descriptorBuilder);

    protected final Descriptor getSourceDescriptor() {
        return sourceDescriptor;
    }

}
