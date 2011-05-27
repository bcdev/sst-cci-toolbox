/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * Rescaling applicable to arc3 sea surface temperature columns.
 *
 * @author Thomas Storm
 */
final class ToArc3SeaSurfaceTemperature extends AbstractRescalingToShort {

    ToArc3SeaSurfaceTemperature() {
        super(0.01, 273.15);
    }

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder.unit(Constants.UNIT_SEA_SURFACE_TEMPERATURE);
        targetColumnBuilder.longName("sea surface skin temperature");
        targetColumnBuilder.standardName("sea_surface_skin_temperature");
    }
}
