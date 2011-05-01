package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.VariableDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RightAssociativeCompositionTest {

    @Test
    public void testAssociativity() throws RuleException {
        final VariableDescriptor sourceDescriptor = new VariableDescriptor();

        assertNull(sourceDescriptor.getRole());

        final Rule a = new DescriptorModification() {

            @Override
            public Descriptor apply(Descriptor sourceDescriptor) {
                final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
                targetDescriptor.setName("a");

                return targetDescriptor;
            }
        };
        final Rule b = new DescriptorModification() {

            @Override
            public Descriptor apply(Descriptor sourceDescriptor) {
                final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
                targetDescriptor.setName("b");
                targetDescriptor.setRole("b");

                return targetDescriptor;
            }
        };
        final Rule ab = new RightAssociativeComposition(a, b);
        final Descriptor targetDescriptor = ab.apply(sourceDescriptor);

        assertEquals("a", targetDescriptor.getName());
        assertEquals("b", targetDescriptor.getRole());
    }
}
