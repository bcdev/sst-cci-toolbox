package org.esa.cci.sst.rules;


import org.esa.cci.sst.tools.Constants;

import java.util.Map;

class InsituHelper {

    static int[] getShape(Context context) {
        final Map<String, Integer> dimensionConfiguration = context.getDimensionConfiguration();
        final Integer yDimension = dimensionConfiguration.get(Constants.DIMENSION_NAME_INSITU_NTIME);

        return new int[]{1, yDimension};
    }
}
