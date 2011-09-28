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
import org.esa.cci.sst.data.Observation;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Sets the record number to the corresponding observation's record number, if exists.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
class MdRecordNumber extends Rule {

    private static final short FILL_VALUE = (short) -1;

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn)
                .fillValue(FILL_VALUE)
                .build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.SHORT, new int[]{1});
        final Observation observation = getContext().getObservation();
        if (observation != null) {
            array.setShort(0, (short) observation.getRecordNo());
        } else {
            array.setShort(0, FILL_VALUE);
        }
        return array;
    }
}
