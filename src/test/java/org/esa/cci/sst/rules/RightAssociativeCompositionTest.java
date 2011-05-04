package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.DescriptorBuilder;
import org.esa.cci.sst.data.VariableDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RightAssociativeCompositionTest {

    @Test
    public void testAssociativity() throws RuleException {
        final Rule a = new DescriptorModification() {

            @Override
            public VariableDescriptor apply(VariableDescriptor sourceDescriptor) {
                return new DescriptorBuilder(sourceDescriptor).setName("a").build();
            }
        };
        final Rule b = new DescriptorModification() {

            @Override
            public VariableDescriptor apply(VariableDescriptor sourceDescriptor) {
                return new DescriptorBuilder(sourceDescriptor).setName("b").setRole("b").build();
            }
        };
        final Rule ab = new RightAssociativeComposition(a, b);
        final DescriptorBuilder descriptorBuilder = new DescriptorBuilder();
        final VariableDescriptor sourceDescriptor = descriptorBuilder.build();
        final VariableDescriptor targetDescriptor = ab.apply(sourceDescriptor);

        assertEquals("a", targetDescriptor.getName());
        assertEquals("b", targetDescriptor.getRole());
    }
}
