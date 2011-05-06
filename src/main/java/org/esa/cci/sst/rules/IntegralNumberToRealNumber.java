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
import org.esa.cci.sst.data.ColumnI;
import ucar.ma2.DataType;

import java.text.MessageFormat;

/**
 * Base class some for numeric conversions.
 *
 * @author Ralf Quast
 */
public abstract class IntegralNumberToRealNumber implements Rule {

    @Override
    public ColumnI apply(ColumnI sourceColumn) throws RuleException {
        final DataType sourceDataType = getSourceDataType();
        Assert.condition(sourceDataType.isIntegral(),
                         MessageFormat.format("Expected integral numeric type, actual type is ''{0}''.",
                                              sourceDataType.name()));
        Assert.type(sourceDataType, sourceColumn);

        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        builder.setAddOffset(null);
        builder.setScaleFactor(null);
        builder.setType(getTargetDataType());
        builder.setFillValue(null);

        final Number sourceFillValue = sourceColumn.getFillValue();
        if (sourceFillValue != null) {
            switch (sourceDataType) {
            case BYTE:
                builder.setFillValue(apply(sourceFillValue.byteValue(), sourceColumn));
                break;
            case SHORT:
                builder.setFillValue(apply(sourceFillValue.shortValue(), sourceColumn));
                break;
            case INT:
                builder.setFillValue(apply(sourceFillValue.intValue(), sourceColumn));
                break;
            case LONG:
                builder.setFillValue(apply(sourceFillValue.longValue(), sourceColumn));
                break;
            }
        }

        return builder.build();
    }

    @Override
    public Number apply(Number number, ColumnI sourceColumn) throws RuleException {
        Number sourceAddOffset = sourceColumn.getAddOffset();
        Number sourceScaleFactor = sourceColumn.getScaleFactor();
        if (sourceScaleFactor == null) {
            sourceScaleFactor = 1.0;
        }
        if (sourceAddOffset == null) {
            sourceAddOffset = 0.0;
        }

        return computeTargetNumber(number, sourceAddOffset, sourceScaleFactor);
    }

    protected abstract DataType getTargetDataType();

    protected abstract DataType getSourceDataType();

    protected abstract Number computeTargetNumber(Number number,
                                                  Number sourceAddOffset,
                                                  Number sourceScaleFactor) throws RuleException;
}
