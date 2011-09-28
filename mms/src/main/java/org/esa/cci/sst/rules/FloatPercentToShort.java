/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import ucar.ma2.IndexIterator;

/**
 * Rule to convert a percentage value given in float to a short in [0..10000].
 *
 * @author Thomas Storm
 */
class FloatPercentToShort extends AbstractReformat<Float, Short> {

    protected FloatPercentToShort() {
        super(Float.class, Short.class);
    }

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        // no additional configuration needed
    }

    @Override
    protected void apply(Array sourceArray, Array targetArray, Number scaleFactor, Number addOffset) {
        final IndexIterator sourceIterator = sourceArray.getIndexIterator();
        final IndexIterator targetIterator = targetArray.getIndexIterator();
        while(sourceIterator.hasNext() && targetIterator.hasNext()) {
            final float currentValue = sourceIterator.getFloatNext();
            targetIterator.setShortNext((short) (currentValue * 100));
        }
    }
}
