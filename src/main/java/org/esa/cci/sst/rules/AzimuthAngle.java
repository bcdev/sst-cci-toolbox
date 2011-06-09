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

/**
 * Rescales the zenith angle.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
class AzimuthAngle extends AbstractRescalingToShort {

    protected AzimuthAngle() {
        super(0.01, 0.0);
    }

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder) {
        targetColumnBuilder
                .fillValue(-32768)
                .validMin(-180.0)
                .validMax(180.0);
    }
}
