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
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Date;

/**
 * Sets the reference time.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class ReferenceTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;
    private static final int[] SHAPE = new int[]{1};

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DATA_TYPE, SHAPE);
        array.setInt(0, getReferenceTime());
        return array;
    }

    private int getReferenceTime() throws RuleException {
        final Date date = getContext().getMatchup().getRefObs().getTime();
        return (int) TimeUtil.toJulianDate(date);
    }

}
