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

package org.esa.beam.util;

import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;


/**
 * A simple facade wrapping a {@link Variable}.
 *
 * @author Ralf Quast
 */
final class VariableSampleSource implements SampleSource {

    private Array data;
    private final int width;
    private final int height;
    private final double addOffset;
    private final double scalingFactor;
    private final double fillValue;

    VariableSampleSource(Variable variable, Array slice) {
        data = slice;
        width = variable.getShape(variable.getRank() - 1);
        height = variable.getShape(variable.getRank() - 2);
        scalingFactor = getAttribute(variable, "scale_factor", 1.0);
        addOffset = getAttribute(variable, "add_offset", 0.0);
        fillValue = getAttribute(variable, "_FillValue", Double.NEGATIVE_INFINITY);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public double getSample(int x, int y) {
        final double sample = data.getDouble(x + y * width);
        return sample != fillValue ? sample * scalingFactor + addOffset : Double.NaN;
    }

    @Override
    public boolean isFillValue(int x, int y) {
        final double sample = data.getDouble(x + y * width);
        return sample == fillValue || Double.isNaN(sample);
    }

    @Override
    public void dispose() {
        data = null;
    }

    // @todo 3 tb/** duplicated code? I'm sure this functionality already exists in the project tb 2015-05-19
    private static double getAttribute(Variable v, String name, double defaultValue) {
        final Attribute attribute = v.findAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getNumericValue(0).doubleValue();
    }
}
