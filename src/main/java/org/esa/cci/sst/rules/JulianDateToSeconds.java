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

/**
 * Converts times (Julian Date) into seconds since 1978-01-01 00:00:00.
 */
final class JulianDateToSeconds implements Rule {

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        Assert.type(DataType.DOUBLE, sourceColumn);
        Assert.unit("Julian Date", sourceColumn);

        final ColumnBuilder builder = new ColumnBuilder(sourceColumn);
        builder.unit("seconds since 1978-01-01 00:00:00");
        final Number sourceFillValue = sourceColumn.getFillValue();
        if (sourceFillValue != null) {
            builder.fillValue((sourceFillValue.doubleValue() - 2443509.5) * 86400.0);
        }

        return builder.build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        Assert.type(DataType.DOUBLE, sourceArray);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            sourceArray.setDouble(i, (sourceArray.getDouble(i) - 2443509.5) * 86400.0);
        }
        return sourceArray;
    }

}
