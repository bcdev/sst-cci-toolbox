package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.tools.Constants;

/**
 * Rescaling applicable for brightness temperature columns.
 *
 * @author Ralf Quast
 */
final class ToBrightnessTemperature extends AbstractRescaling {

    ToBrightnessTemperature() {
        super(1.0, 0.0);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder.unit(Constants.UNIT_BRIGHTNESS_TEMPERATURE);
    }
}
