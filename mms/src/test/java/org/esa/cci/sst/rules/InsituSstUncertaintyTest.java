package org.esa.cci.sst.rules;

import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.mmdgeneration.ContextBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class InsituSstUncertaintyTest {

    @Test
    public void testSetContext_historyShapeIsSetFromDimensionMap() {
        final Map<String, Integer> dimensionConfiguration = new HashMap<>();
        dimensionConfiguration.put(Constants.DIMENSION_NAME_INSITU_NTIME, 83);

        final Context context = new ContextBuilder().dimensionConfiguration(dimensionConfiguration).build();

        final InsituSstUncertainty insituSstUncertainty = new InsituSstUncertainty();
        insituSstUncertainty.setContext(context);

        assertNotNull(insituSstUncertainty.historyShape);
        assertArrayEquals(new int[]{1, 83}, insituSstUncertainty.historyShape);
    }
}
