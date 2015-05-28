/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.tools.Constants;

/**
 * Rescaling applicable to brightness temperature columns for microwave sensors (AMSR2).
 *
 * @author Ralf Quast
 */
final class ToBrightnessTemperatureM extends AbstractRescalingToShort {
    ToBrightnessTemperatureM() {
        super(0.005, 180.0);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder
                .unit(Constants.UNIT_BRIGHTNESS_TEMPERATURE)
                .fillValue(-32768)
                .validMin(-26000.0)
                .validMax(28000.0)
                .longName(null);
    }
}
