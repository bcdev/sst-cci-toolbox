/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.util;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * NetCDF utility functions.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class NcUtils {

    public static Variable getVariable(NetcdfFile netcdfFile, String variableName) throws IOException {
        final Variable variable = netcdfFile.findVariable(variableName);
        if (variable == null) {
            throw new IOException(String.format("Missing variable '%s' in file '%s'.", variableName, netcdfFile.getLocation()));
        }
        return variable;
    }

    public static boolean hasVariable(NetcdfFile netcdfFile, String variableName) {
        return netcdfFile.findVariable(variableName) != null;
    }

    public static Grid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef) throws IOException {
        return readGrid(netcdfFile, variableName, expectedGridDef, 0);
    }

    public static Grid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef, int z) throws IOException {
        final Variable variable = getVariable(netcdfFile, variableName);
        return readGrid(netcdfFile, variable, expectedGridDef, z);
    }

    private static double getAddOffset(Variable variable) {
        return getNumericAttributeValue(variable, "add_offset", 0.0);
    }

    private static double getScaleFactor(Variable variable) {
        return getNumericAttributeValue(variable, "scale_factor", 1.0);
    }

    private static Number getFillValue(Variable variable) {
        return getNumericAttributeValue(variable, "_FillValue");
    }

    private static double getNumericAttributeValue(Variable variable, String name, double defaultValue) {
        Number value = getNumericAttributeValue(variable, name);
        if (value != null) {
            return value.doubleValue();
        } else {
            return defaultValue;
        }
    }

    private static Number getNumericAttributeValue(Variable variable, String name) {
        Attribute attribute = variable.findAttribute(name);
        if (attribute != null) {
            return attribute.getNumericValue();
        } else {
            return null;
        }
    }

    private static Array readRaster(Variable variable, Rectangle gridRectangle, int z) throws IOException {
        final int rank = variable.getRank();
        if (z > 0) {
            if (rank < 3) {
                throw new IOException(String.format("NetCDF variable '%s': Expected rank 3 or higher, but found %d.",
                        variable.getName(), rank));
            }
        } else if (rank < 2) {
            throw new IOException(String.format("NetCDF variable '%s': Expected rank 2 or higher, but found %d.",
                    variable.getName(), rank));
        }
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        Arrays.fill(shape, 1);
        origin[rank - 1] = gridRectangle.x;
        origin[rank - 2] = gridRectangle.y;
        if (rank > 2) {
            origin[rank - 3] = z;
        }
        shape[rank - 1] = gridRectangle.width;
        shape[rank - 2] = gridRectangle.height;
        final Array array;
        try {
            array = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        }
        return array.reshapeNoCopy(new int[]{gridRectangle.height, gridRectangle.width});
    }

    private static Grid readGrid(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef, int z) throws IOException {
        final Rectangle gridRectangle = getGridRectangle(netcdfFile, variable, expectedGridDef);
        final double scaleFactor = getScaleFactor(variable);
        final double addOffset = getAddOffset(variable);
        final Number fillValue = getFillValue(variable);
        final Array data = readRaster(variable, gridRectangle, z);
        return new ArrayGrid(expectedGridDef, data, fillValue, scaleFactor, addOffset);
    }

    private static Rectangle getGridRectangle(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef) throws IOException {
        final int rank = variable.getRank();
        if (rank < 2) {
            throw new IOException(String.format("Variable '%s' in file '%s': Expected rank 2 or higher, but found %d.",
                    variable.getName(), netcdfFile.getLocation(), rank));
        }
        final int w = variable.getDimension(rank - 1).getLength();
        final int h = variable.getDimension(rank - 2).getLength();
        if (w != expectedGridDef.getWidth() || h != expectedGridDef.getHeight()) {
            throw new IOException(String.format("Variable '%s' in file '%s': Unexpected grid size.", variable.getName(), netcdfFile.getLocation()));
        }
        return new Rectangle(0, 0, w, h);
    }
}
