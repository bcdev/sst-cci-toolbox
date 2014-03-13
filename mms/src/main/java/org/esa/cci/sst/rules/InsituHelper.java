package org.esa.cci.sst.rules;


import java.util.Map;

class InsituHelper {

    static int[] getShape(Context context) {
        final Map<String, Integer> dimensionConfiguration = context.getDimensionConfiguration();
        final Integer yDimension = dimensionConfiguration.get("insitu.time");

        return new int[]{1, yDimension};
    }
}
