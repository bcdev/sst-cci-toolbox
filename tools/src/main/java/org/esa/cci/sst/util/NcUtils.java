package org.esa.cci.sst.util;

import org.esa.cci.sst.regavg.Climatology;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Rectangle;
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

    public static Array readRaster(Variable variable, Rectangle gridRectangle, int t) throws IOException {
        int rank = variable.getRank();
        int[] origin = new int[rank];
        int[] shape = new int[rank];
        Arrays.fill(shape, 1);
        origin[rank-1] = gridRectangle.x;
        origin[rank-2] = gridRectangle.y;
        origin[rank-3] = t;
        shape[rank-1] = gridRectangle.width;
        shape[rank-2] = gridRectangle.height;
        Array array;
        try {
            array = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    public static ArrayGrid readGrid(NetcdfFile netcdfFile, String variableName, int t, GridDef expectedGridDef) throws IOException {
        Variable variable = netcdfFile.findTopVariable(variableName);
        if (variable == null) {
            throw new IOException(String.format("Missing variable '%s' in file '%s'.", variableName, netcdfFile.getLocation()));
        }
        int rank = variable.getRank();
        int width = variable.getDimension(rank - 1).getLength();
        int height = variable.getDimension(rank - 2).getLength();
        if (width != expectedGridDef.getWidth() || height != expectedGridDef.getHeight()) {
            throw new IOException(String.format("Variable '%s' in file '%s': Unexpected grid size.", variableName, netcdfFile.getLocation()));
        }
        // todo - check lat/lon fields to make sure they fit to expectedGridDef
        double scaleFactor = getScaleFactor(variable);
        double addOffset = getAddOffset(variable);
        Number fillValue = getFillValue(variable);
        Array data = readRaster(variable, new Rectangle(0, 0, width, height), t);
        return new ArrayGrid(expectedGridDef, scaleFactor, addOffset, fillValue, data);
    }
}
