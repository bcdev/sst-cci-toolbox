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

import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.InsituSource;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the time of the insitu observation, if present. Otherwise, the reference observation's time is set.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class InsituTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;
    private static final int FILL_VALUE = Integer.MIN_VALUE;
    private static final int[] SINGLE_VALUE_SHAPE = {1, 1};
    // package access for testing only tb 2014-03-13
    int[] historyShape;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder
                .type(DATA_TYPE)
                .fillValue(FILL_VALUE)
                .unit(Constants.UNIT_INSITU_TIME);
    }

    @Override
    public final Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Context context = getContext();
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final double refTime = TimeUtil.dateToSecondsSinceEpoch(refObs.getTime());
        final Reader observationReader = context.getObservationReader();
        try {
            if (observationReader != null) {
                final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                        .shape(historyShape)
                        // TODO .halfExtractDuration(something)
                        .referenceObservation(refObs)
                        .build();
                final Array insituTimes = observationReader.read("insitu.time", extractDefinition);
                for (int i = 0; i < insituTimes.getSize(); i++) {
                    final double insituTime = insituTimes.getDouble(i);
                    insituTimes.setDouble(i, insituTime - refTime);
                }
                return insituTimes;
            } else {
                final Array array = Array.factory(DATA_TYPE, SINGLE_VALUE_SHAPE);
                final InsituSource insituSource = context.getReferenceObservationReader().getInsituSource();
                if (insituSource != null) {
                    final double insituTime = insituSource.readInsituTime(refObs.getRecordNo());
                    array.setDouble(0, insituTime - refTime);
                }
                return array;
            }
        } catch (IOException e) {
            throw new RuleException("Unable to read in-situ time", e);
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        historyShape = InsituHelper.getShape(context);
    }
}
