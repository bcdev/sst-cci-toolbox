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

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Sets metop.dtime.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class MetopDTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.SHORT;

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE).unit(Constants.UNIT_TIME);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array metopDTimes = readMetopDTimes();
        final Array array = Array.factory(DATA_TYPE, metopDTimes.getShape());
        for (int i = 0; i < array.getSize(); i++) {
            array.setShort(i, metopDTimes.getShort(i));
        }
        return array;
    }

    private Array readMetopDTimes() throws RuleException {
        final Context context = getContext();
        final Variable targetVariable = context.getTargetVariable();
        final Coincidence coincidence = context.getCoincidence();
        if (targetVariable.getDimensions().size() != 2) {
            return null;
        }
        final int rowCount = targetVariable.getDimension(1).getLength();
        if (coincidence == null || !coincidence.getObservation().getSensor().equalsIgnoreCase("metop")) {
            return Array.factory(DataType.SHORT, new int[]{1, rowCount});
        }
        final ReferenceObservation observation = (ReferenceObservation) coincidence.getObservation();
        final Reader reader = context.getCoincidenceReader();
        final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                .referenceObservation(coincidence.getMatchup().getRefObs())
                .recordNo(context.getMatchup().getRefObs().getRecordNo())
                .shape(new int[]{1, rowCount})
                .build();
        try {
            return reader.read("dtime", extractDefinition);
        } catch (IOException e) {
            throw new RuleException("Unable to read variable 'dtime'.", e);
        }
    }

}
