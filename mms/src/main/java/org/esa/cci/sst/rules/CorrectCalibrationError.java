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

import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Corrects the calibration error of brightness temperatures in the AATSR 12 micron channel spectral response function.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"UnusedDeclaration"})
final class CorrectCalibrationError extends Rule {

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return sourceColumn;
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.SHORT, sourceArray.getShape());
        final double correction = 0.2 / sourceColumn.getScaleFactor().doubleValue();
        for (int i = 0; i < sourceArray.getSize(); i++) {
            final Number value = (Number) sourceArray.getObject(i);
            if (!value.equals(sourceColumn.getFillValue())) {
                array.setShort(i, (short) (value.shortValue() + correction));
            }
        }
        return array;
    }
}
