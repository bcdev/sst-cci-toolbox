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
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Sets dtime.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class DTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.SHORT;
    // todo - ts 06Jun11 - clarify
    private static final int FILL_VALUE = 0;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE).unit(Constants.UNIT_DTIME);
        targetColumnBuilder.fillValue(FILL_VALUE);
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Variable targetVariable = getContext().getTargetVariable();
        final int rowCount = targetVariable.getDimension(1).getLength();
        final Array array = Array.factory(DATA_TYPE, new int[]{1, rowCount});
        for (int i = 0; i < array.getSize(); i++) {
            array.setShort(i, (short) getDTime(i));
        }
        return array;
    }

    private double getDTime(int scanLine) throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getObservationReader();
        if (reader == null) {
            return FILL_VALUE;
        }
        final int recordNo = context.getObservation().getRecordNo();
        try {
            return reader.getDTime(recordNo, scanLine);
        } catch (IOException e) {
            throw new RuleException("Unable to read dtime.", e);
        }
    }
}
