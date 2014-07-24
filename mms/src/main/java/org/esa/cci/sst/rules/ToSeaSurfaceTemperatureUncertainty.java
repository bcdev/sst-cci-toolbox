package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.tools.Constants;

/**
 * @author Ralf Quast
 */
final class ToSeaSurfaceTemperatureUncertainty extends AbstractRescalingToShort {

    ToSeaSurfaceTemperatureUncertainty() {
        super(0.001, 0.0);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder
                .unit(Constants.UNIT_SEA_SURFACE_TEMPERATURE)
                .validMin(-22000.0)
                .validMax(22000.0)
                .longName(null);
    }


}
