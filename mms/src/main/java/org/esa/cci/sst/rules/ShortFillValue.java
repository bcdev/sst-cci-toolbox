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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.Reader;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Sets the detector temperature.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
class ShortFillValue extends Rule {

    private static final short FILL_VALUE = Short.MIN_VALUE;
    private static final DataType DATA_TYPE = DataType.SHORT;

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return sourceColumn;
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array result = Array.factory(DATA_TYPE, new int[]{1});
        result.setShort(0, FILL_VALUE);
        return result;
    }
}
