package org.esa.cci.sst.util;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;

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
}
