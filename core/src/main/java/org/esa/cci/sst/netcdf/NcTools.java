package org.esa.cci.sst.netcdf;

import org.esa.cci.sst.common.ArrayGrid;
import org.esa.cci.sst.common.Grid;
import org.esa.cci.sst.common.GridDef;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class NcTools {

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

    // @todo 3 tb/tb write test 2014-11-10
    public static Grid readGrid(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef, int z) throws IOException {
        final Rectangle gridRectangle = getGridRectangle(netcdfFile, variable, expectedGridDef);
        final double scaleFactor = getScaleFactor(variable);
        final double addOffset = getAddOffset(variable);
        final Number fillValue = getFillValue(variable);
        final Array data = readRaster(variable, gridRectangle, z);

        return new ArrayGrid(expectedGridDef, data, fillValue, scaleFactor, addOffset);
    }

    // @todo 3 tb/tb write test 2014-11-10
    public static Grid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef, int z) throws IOException {
        final Variable variable = getVariable(netcdfFile, variableName);
        return readGrid(netcdfFile, variable, expectedGridDef, z);
    }

    // @todo 3 tb/tb write test 2014-11-10
    public static Grid readGrid(NetcdfFile netcdfFile, String variableName, GridDef expectedGridDef) throws IOException {
        return readGrid(netcdfFile, variableName, expectedGridDef, 0);
    }

    // package access for testing only tb 2014-11-10
    static Rectangle getGridRectangle(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef) throws IOException {
        final int rank = variable.getRank();
        if (rank < 2) {
            throw new IOException(String.format("Variable '%s' in file '%s': expected rank 2 or higher, but found %d.",
                    variable.getShortName(), netcdfFile.getLocation(), rank));
        }

        final int w = variable.getDimension(rank - 1).getLength();
        final int h = variable.getDimension(rank - 2).getLength();
        if (w != expectedGridDef.getWidth() || h != expectedGridDef.getHeight()) {
            throw new IOException(String.format("Variable '%s' in file '%s': unexpected grid size.", variable.getShortName(), netcdfFile.getLocation()));
        }
        return new Rectangle(0, 0, w, h);
    }

    // package access for testing only tb 2014-11-10
    static Number getNumericAttributeValue(Variable variable, String name) {
        final Attribute attribute = variable.findAttribute(name);
        if (attribute != null) {
            return attribute.getNumericValue();
        } else {
            return null;
        }
    }

    // package access for testing only tb 2014-11-10
    static double getNumericAttributeValue(Variable variable, String name, double defaultValue) {
        Number value = getNumericAttributeValue(variable, name);
        if (value != null) {
            return value.doubleValue();
        } else {
            return defaultValue;
        }
    }

    // package access for testing only tb 2014-11-10
    static double getScaleFactor(Variable variable) {
        return getNumericAttributeValue(variable, "scale_factor", 1.0);
    }

    // package access for testing only tb 2014-11-10
    static double getAddOffset(Variable variable) {
        return getNumericAttributeValue(variable, "add_offset", 0.0);
    }

    // package access for testing only tb 2014-11-10
    static Number getFillValue(Variable variable) {
        return getNumericAttributeValue(variable, "_FillValue");
    }

    // @todo 3 tb/tb write test 2014-11-10
    private static Array readRaster(Variable variable, Rectangle gridRectangle, int z) throws IOException {
        final int rank = variable.getRank();
        if (z > 0) {
            if (rank < 3) {
                throw new IOException(String.format("NetCDF variable '%s': Expected rank 3 or higher, but found %d.",
                        variable.getShortName(), rank));
            }
        } else if (rank < 2) {
            throw new IOException(String.format("NetCDF variable '%s': Expected rank 2 or higher, but found %d.",
                    variable.getShortName(), rank));
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
            throw new IOException(e);
        }
        return array.reshapeNoCopy(new int[]{gridRectangle.height, gridRectangle.width});
    }
}
