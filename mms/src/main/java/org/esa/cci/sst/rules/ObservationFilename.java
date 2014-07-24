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
import ucar.ma2.Index;

import java.io.File;

/**
 * Sets the filename.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
final class ObservationFilename extends Rule {

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn)
                .type(DataType.CHAR)
                .build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.CHAR, new int[]{1, 80});
        if (getContext() == null || getContext().getObservation() == null) {
            return sourceArray;
        }
        Observation observation = getContext().getObservation();
        final String filePath = observation.getDatafile().getPath();
        final String filename = new File(filePath).getName();
        final Index index = array.getIndex();
        for (int i = 0; i < filename.length(); i++) {
            index.set(0, i);
            array.setChar(index, filename.charAt(i));
        }

        return array;
    }
}
