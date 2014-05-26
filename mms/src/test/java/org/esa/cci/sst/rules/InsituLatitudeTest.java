package org.esa.cci.sst.rules;


import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.mmdgeneration.ContextBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class InsituLatitudeTest {

    @Test
    public void testSetContext_historyShapeIsSetFromDimensionMap() {
        final Map<String, Integer> dimensionConfiguration = new HashMap<>();
        dimensionConfiguration.put(Constants.DIMENSION_NAME_INSITU_NTIME, 65);

        final Context context = new ContextBuilder().dimensionConfiguration(dimensionConfiguration).build();

        final InsituLatitude insituLatitude = new InsituLatitude();
        insituLatitude.setContext(context);

        assertNotNull(insituLatitude.historyShape);
        assertArrayEquals(new int[]{1, 65}, insituLatitude.historyShape);
    }
}
