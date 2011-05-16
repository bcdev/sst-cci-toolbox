package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.tools.Constants;

/**
 * Rescaling applicable to sea surface temperature columns.
 *
 * @author Ralf Quast
 */
final class ToSeaSurfaceTemperature extends AbstractRescaling {

    ToSeaSurfaceTemperature() {
        super(1.0, 0.0);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder.unit(Constants.UNIT_SEA_SURFACE_TEMPERATURE);
    }
}
