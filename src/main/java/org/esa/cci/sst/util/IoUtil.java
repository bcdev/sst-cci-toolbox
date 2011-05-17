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
import ucar.ma2.Array;
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
        builder.name(sensorName + '.' + variable.getName());
        builder.type(variable.getDataType());
        builder.unsigned(variable.isUnsigned());
        builder.rank(variable.getRank());
        builder.dimensions(variable.getDimensionsString());
        setUnit(variable, builder);
        setFlagMasks(variable, builder);
        setFlagMeanings(variable, builder);
        setFlagValues(variable, builder);
        setAttributes(variable, builder);
        builder.role(variable.getName());

        return builder;
    }

    private static void setUnit(final Variable variable, final ColumnBuilder builder) {
        final String unit = variable.getUnitsString();
        if (unit != null && !unit.isEmpty()) {
            builder.unit(unit);
        }
    }

    private static void setFlagMasks(final Variable variable, final ColumnBuilder cb) {
        final Attribute attribute = variable.findAttribute("flag_masks");
        if (attribute != null) {
            final Array values = attribute.getValues();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < attribute.getLength(); i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(values.getInt(i));
            }
            cb.flagMasks(sb.toString());
        }
    }

    private static void setFlagMeanings(final Variable variable, final ColumnBuilder builder) {
        final Attribute attribute = variable.findAttribute("flag_meanings");
        if (attribute != null) {
            builder.flagMeanings(attribute.getStringValue());
        }
    }

    private static void setFlagValues(final Variable variable, final ColumnBuilder cb) {
        final Attribute attribute = variable.findAttribute("flag_values");
        if (attribute != null) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < attribute.getLength(); i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(attribute.getNumericValue(i));
            }
            cb.flagValues(sb.toString());
        }
    }

    private static void setAttributes(final Variable variable, final ColumnBuilder builder) {
        for (final Attribute attribute : variable.getAttributes()) {
            if ("add_offset".equals(attribute.getName())) {
                builder.addOffset(attribute.getNumericValue());
            }
            if ("scale_factor".equals(attribute.getName())) {
                builder.scaleFactor(attribute.getNumericValue());
            }
            if ("_FillValue".equals(attribute.getName())) {
                builder.fillValue(attribute.getNumericValue());
            }
            if ("valid_min".equals(attribute.getName())) {
                builder.validMin(attribute.getNumericValue());
            }
            if ("valid_max".equals(attribute.getName())) {
                builder.validMax(attribute.getNumericValue());
            }
            if ("valid_range".equals(attribute.getName())) {
                builder.validMin(attribute.getNumericValue(0));
                builder.validMax(attribute.getNumericValue(1));
            }
            if ("long_name".equals(attribute.getName())) {
                builder.longName(attribute.getStringValue());
            }
            if ("standard_name".equals(attribute.getName())) {
                builder.standardName(attribute.getStringValue());
            }
        }
    }

    public static Attribute addFlagValues(Variable v, String name, String valueString) {
        Assert.notNull(v, "v == null");
        Assert.notNull(name, "name == null");
        Assert.notNull(valueString, "value == null");

        final String[] values = valueString.split("\\s");
        final Array array = Array.factory(v.getDataType(), new int[]{values.length});
        array.setUnsigned(v.isUnsigned());
        final Attribute attribute = v.addAttribute(new Attribute(name, array));
        for (int i = 0; i < values.length; i++) {
            array.setInt(i, Integer.valueOf(values[i]));
        }
        return attribute;
    }

    public static Attribute addAttribute(Variable v, String name, String value) {
        Assert.notNull(v, "v == null");
        Assert.notNull(name, "name == null");
        Assert.notNull(value, "value == null");

        return v.addAttribute(new Attribute(name, value));
    }

    public static Attribute addAttribute(Variable v, String name, Number value) {
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

        final boolean unsigned = column.isUnsigned();
        if (unsigned) {
            addAttribute(v, "_Unsigned", "true");
        }
        final String unit = column.getUnit();
        if (unit != null) {
            addAttribute(v, "units", unit);
        }
        final String flagMasks = column.getFlagMasks();
        if (flagMasks != null) {
            addFlagValues(v, "flag_masks", flagMasks);
        }
        final String flagMeanings = column.getFlagMeanings();
        if (flagMeanings != null) {
            addAttribute(v, "flag_meanings", flagMeanings);
        }
        final String flagValues = column.getFlagValues();
        if (flagValues != null) {
            addFlagValues(v, "flag_values", flagValues);
        }
        final Number addOffset = column.getAddOffset();
        if (addOffset != null) {
            addAttribute(v, "add_offset", addOffset);
        }
        final Number scaleFactor = column.getScaleFactor();
        if (scaleFactor != null) {
            addAttribute(v, "scale_factor", scaleFactor);
        }
        final Number fillValue = column.getFillValue();
        if (fillValue != null) {
            addAttribute(v, "_FillValue", fillValue, dataType);
        }
        final Number validMin = column.getValidMin();
        if (fillValue != null) {
            addAttribute(v, "validMin", validMin, dataType);
        }
        final Number validMax = column.getValidMax();
        if (fillValue != null) {
            addAttribute(v, "validMax", validMax, dataType);
        }
        final String standardName = column.getStandardName();
        if (standardName != null) {
            addAttribute(v, "standard_name", standardName);
        }
        final String longName = column.getLongName();
        if (longName != null) {
            addAttribute(v, "long_name", longName);
        }
    }
}
