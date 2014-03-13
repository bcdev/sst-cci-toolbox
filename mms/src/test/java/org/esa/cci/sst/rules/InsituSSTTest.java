package org.esa.cci.sst.rules;


import org.esa.cci.sst.tools.mmdgeneration.ContextBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class InsituSSTTest {

    @Test
    public void testSetContext_historyShapeIsSetFromDimensionMap() {
        final Map<String, Integer> dimensionConfiguration = new HashMap<>();
        dimensionConfiguration.put("insitu.time", 84);

        final Context context = new ContextBuilder().dimensionConfiguration(dimensionConfiguration).build();

        final InsituSST insituSST = new InsituSST();
        insituSST.setContext(context);

        assertNotNull(insituSST.historyShape);
        assertArrayEquals(new int[]{1, 84}, insituSST.historyShape);
    }
}
