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

import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Rule that transforms elevation angles into the respective zenith angles.
 *
 * @author Thomas Storm
 */
public class ElevationToSolzen implements Rule {

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return sourceColumn;
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        Assert.type(DataType.FLOAT, sourceArray);
        for (int i = 0; i < sourceArray.getSize(); i++) {
            sourceArray.setFloat(i, 90.0f - sourceArray.getFloat(i));
        }
        return sourceArray;
    }
}
