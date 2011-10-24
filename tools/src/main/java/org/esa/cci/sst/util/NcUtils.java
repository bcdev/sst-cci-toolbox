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

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

/**
 * NetCDF utility functions.
 *
 * @author Norman
 */
public class NcUtils {

    public static double getAddOffset(Variable variable) {
        return getNumericAttributeValue(variable, "add_offset", 0.0);
    }

    public static double getScaleFactor(Variable variable) {
        return getNumericAttributeValue(variable, "scale_factor", 1.0);
    }

    public static Number getFillValue(Variable variable) {
        return getNumericAttributeValue(variable, "_FillValue");
    }

    public static double getNumericAttributeValue(Variable variable, String name, double defaultValue) {
        Number value = getNumericAttributeValue(variable, name);
        if (value != null) {
            return value.doubleValue();
        } else {
            return defaultValue;
        }
    }

    public static Number getNumericAttributeValue(Variable variable, String name) {
        Attribute attribute = variable.findAttribute(name);
        if (attribute != null) {
            return attribute.getNumericValue();
        } else {
            return null;
        }
    }

    public static Array readRaster(Variable variable, Rectangle gridRectangle, int z) throws IOException {
        int rank = variable.getRank();
        if (z > 0) {
            if (rank < 3) {
                throw new IOException(String.format("NetCDF variable '%s': Expected rank 3 or higher, but found %d.",
                                                    variable.getName(), rank));
            }
        } else if (rank < 2) {
            throw new IOException(String.format("NetCDF variable '%s': Expected rank 2 or higher, but found %d.",
                                                variable.getName(), rank));
        }
        int[] origin = new int[rank];
        int[] shape = new int[rank];
        Arrays.fill(shape, 1);
        origin[rank - 1] = gridRectangle.x;
        origin[rank - 2] = gridRectangle.y;
        if (rank > 2) {
            origin[rank - 3] = z;
        }
        shape[rank - 1] = gridRectangle.width;
        shape[rank - 2] = gridRectangle.height;
        Array array;
        try {
            array = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    public static ArrayGrid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef) throws IOException {
        return readGrid(netcdfFile, variableName, expectedGridDef, 0);
    }

    public static ArrayGrid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef, int z) throws IOException {
        Variable variable = getVariable(netcdfFile, variableName);
        return readGrid(netcdfFile, variable, expectedGridDef, z);
    }

    public static ArrayGrid readGrid(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef, int z) throws IOException {
        Rectangle gridRectangle = getGridRectangle(netcdfFile, variable, expectedGridDef);
        double scaleFactor = getScaleFactor(variable);
        double addOffset = getAddOffset(variable);
        Number fillValue = getFillValue(variable);
        Array data = readRaster(variable, gridRectangle, z);
        return new ArrayGrid(expectedGridDef, scaleFactor, addOffset, fillValue, data);
    }

    public static Rectangle getGridRectangle(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef) throws IOException {
        int rank = variable.getRank();
        if (rank < 2) {
            throw new IOException(String.format("Variable '%s' in file '%s': Expected rank 2 or higher, but found %d.",
                                                variable.getName(), netcdfFile.getLocation(), rank));
        }
        int width = variable.getDimension(rank - 1).getLength();
        int height = variable.getDimension(rank - 2).getLength();
        if (width != expectedGridDef.getWidth() || height != expectedGridDef.getHeight()) {
            throw new IOException(String.format("Variable '%s' in file '%s': Unexpected grid size.", variable.getName(), netcdfFile.getLocation()));
        }
        // todo - check lat/lon fields to make sure they fit to expectedGridDef
        return new Rectangle(0, 0, width, height);
    }

    public static Variable getVariable(NetcdfFile netcdfFile, String variableName) throws IOException {
        Variable variable = netcdfFile.findTopVariable(variableName);
        if (variable == null) {
            throw new IOException(String.format("Missing variable '%s' in file '%s'.", variableName, netcdfFile.getLocation()));
        }
        return variable;
    }

}
