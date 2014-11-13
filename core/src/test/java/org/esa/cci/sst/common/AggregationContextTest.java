package org.esa.cci.sst.common;

import org.junit.Test;

import static org.junit.Assert.fail;

public class AggregationContextTest {

    @Test
    public void testGetSourceGridDef_throwsWhenNull() {
        final AggregationContext context = new AggregationContext();

        context.setSstGrid(null);

        try {
            context.getSourceGridDef();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }
}
