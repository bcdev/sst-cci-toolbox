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
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.text.MessageFormat;

/**
 * Abstract base class for numeric conversion rules.
 *
 * @author Ralf Quast
 */
abstract class AbstractReformat<S extends Number, T extends Number> extends Rule {

    private final DataType sourceDataType;
    private final DataType targetDataType;
    private final boolean unsigned;

    protected AbstractReformat(Class<S> sourceType, Class<T> targetType) {
        this(sourceType, targetType, false);
    }

    protected AbstractReformat(Class<S> sourceType, Class<T> targetType, boolean unsigned) {
        this.sourceDataType = getDataType(sourceType);
        this.targetDataType = getDataType(targetType);
        this.unsigned = unsigned;
    }

    @Override
    public final Item apply(Item sourceColumn) throws RuleException {
        Assert.type(sourceDataType, sourceColumn);

        final ColumnBuilder targetColumnBuilder = new ColumnBuilder(sourceColumn);
        targetColumnBuilder.addOffset(null);
        targetColumnBuilder.scaleFactor(null);
        targetColumnBuilder.type(targetDataType);
        configureTargetColumn(targetColumnBuilder, sourceColumn);

        return targetColumnBuilder.build();
    }

    protected abstract void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn);

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        Assert.type(sourceDataType, sourceArray);
        final Array targetArray = Array.factory(targetDataType, sourceArray.getShape());
        targetArray.setUnsigned(unsigned);
        apply(sourceArray, targetArray, sourceColumn.getScaleFactor(), sourceColumn.getAddOffset());

        return targetArray;
    }

    protected abstract void apply(Array sourceArray, Array targetArray, Number scaleFactor, Number addOffset);

    private DataType getDataType(Class<? extends Number> type) {
        if (type == Byte.class) {
            return DataType.BYTE;
        }
        if (type == Short.class) {
            return DataType.SHORT;
        }
        if (type == Integer.class) {
            return DataType.INT;
        }
        if (type == Long.class) {
            return DataType.LONG;
        }
        if (type == Float.class) {
            return DataType.FLOAT;
        }
        if (type == Double.class) {
            return DataType.DOUBLE;
        }
        throw new IllegalArgumentException(
                MessageFormat.format("Expected a simple numeric type, actual type is ''{0}''.",
                                     type.getSimpleName()));
    }
}
