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

package org.esa.cci.sst.util;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.text.MessageFormat;

/**
 * Utility class for commonly used functions.
 *
 * @author Thomas Storm
 */
public class IoUtil {

    private IoUtil() {
    }

    public static ColumnBuilder createColumnBuilder(final Variable variable, final String sensorName) {
        final ColumnBuilder builder = new ColumnBuilder();
        builder.setName(sensorName + '.' + variable.getName());
        builder.setType(variable.getDataType());
        builder.setUnsigned(variable.isUnsigned());
        builder.setRank(variable.getRank());
        builder.setDimensions(variable.getDimensionsString());
        setUnit(variable, builder);
        setAttributes(variable, builder);
        builder.setRole(variable.getName());

        return builder;
    }

    private static void setUnit(final Variable variable, final ColumnBuilder builder) {
        final String unit = variable.getUnitsString();
        if (unit != null && !unit.isEmpty()) {
            builder.setUnit(unit);
        }
    }

    private static void setAttributes(final Variable variable, final ColumnBuilder builder) {
        for (final Attribute attribute : variable.getAttributes()) {
            if ("add_offset".equals(attribute.getName())) {
                builder.setAddOffset(attribute.getNumericValue());
            }
            if ("scale_factor".equals(attribute.getName())) {
                builder.setScaleFactor(attribute.getNumericValue());
            }
            if ("_FillValue".equals(attribute.getName())) {
                builder.setFillValue(attribute.getNumericValue());
            }
            if ("valid_min".equals(attribute.getName())) {
                builder.setValidMin(attribute.getNumericValue());
            }
            if ("valid_max".equals(attribute.getName())) {
                builder.setValidMax(attribute.getNumericValue());
            }
            if ("long_name".equals(attribute.getName())) {
                builder.setLongName(attribute.getStringValue());
            }
            if ("standard_name".equals(attribute.getName())) {
                builder.setStandardName(attribute.getStringValue());
            }
        }
    }

    public static Attribute addAttribute(Variable v, String name, String value) {
        Assert.notNull(v, "v == null");
        Assert.notNull(name, "name == null");
        Assert.notNull(value, "value == null");

        return v.addAttribute(new Attribute(name, value));
    }

    public static Attribute addAttribute(Variable v, String name, Number value, DataType type) {
        Assert.notNull(v, "v == null");
        Assert.notNull(name, "name == null");
        Assert.notNull(value, "value == null");
        Assert.notNull(type, "type == null");

        switch (type) {
            case BYTE:
                return v.addAttribute(new Attribute(name, value.byteValue()));
            case SHORT:
                return v.addAttribute(new Attribute(name, value.shortValue()));
            case INT:
                return v.addAttribute(new Attribute(name, value.intValue()));
            case FLOAT:
                return v.addAttribute(new Attribute(name, value.floatValue()));
            case DOUBLE:
                return v.addAttribute(new Attribute(name, value.doubleValue()));
            default:
                throw new IllegalArgumentException(MessageFormat.format(
                        "Attribute type ''{0}'' is not supported", type.toString()));
        }
    }

    public static void addVariable(NetcdfFileWriteable targetFile, Item column) {
        final Group rootGroup = targetFile.getRootGroup();
        final DataType dataType = DataType.valueOf(column.getType());
        final Variable v = targetFile.addVariable(rootGroup, column.getName(), dataType, column.getDimensions());

        final String standardName = column.getStandardName();
        if (standardName != null) {
            addAttribute(v, "standard_name", standardName);
        }
        final String unit = column.getUnit();
        if (unit != null) {
            addAttribute(v, "units", unit);
        }
        final Number addOffset = column.getAddOffset();
        if (addOffset != null) {
            addAttribute(v, "add_offset", addOffset, DataType.FLOAT);
        }
        final Number scaleFactor = column.getScaleFactor();
        if (scaleFactor != null) {
            addAttribute(v, "scale_factor", scaleFactor, DataType.FLOAT);
        }
        final Number fillValue = column.getFillValue();
        if (fillValue != null) {
            addAttribute(v, "_FillValue", fillValue, dataType);
        }
    }
}
