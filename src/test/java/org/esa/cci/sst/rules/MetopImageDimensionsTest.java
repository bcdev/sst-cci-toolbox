package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.VariableDescriptor;

import static org.junit.Assert.assertEquals;

public class MetopImageDimensionsTest extends AbstractRuleTest {

    @Override
    public void testNumericConversion() throws RuleException {
        // no numeric conversion is carried out for this rule
    }

    @Override
    protected void assertTargetDescriptor(Descriptor targetDescriptor) {
        assertEquals("n metop.ni metop.nj", targetDescriptor.getDimensions());
    }

    @Override
    protected void configureSourceDescriptor() {
        final VariableDescriptor sourceDescriptor = getSourceDescriptor();
        sourceDescriptor.setDimensions("n ny nx");
    }
}
