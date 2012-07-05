/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.regrid;

import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;

import java.util.Arrays;

public class SpatialResolution {

    private static final double[] RESOLUTIONS = new double[]{
            0.05,
            0.10,
            0.15,
            0.20,
            0.25,
            0.30,
            0.40,
            0.50,
            0.60,
            0.75,
            0.80,
            1.00,
            1.20,
            1.25,
            1.50,
            2.00,
            2.25,
            2.40,
            2.50,
            3.00,
            3.75,
            4.00,
            4.50,
            5.00,
            10.0
    };


    public static double[] getValueSet() {
        return Arrays.copyOf(RESOLUTIONS, RESOLUTIONS.length);
    }

    private SpatialResolution() {
    }
}
