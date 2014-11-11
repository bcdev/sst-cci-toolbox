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
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tool.Configuration;
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
final class InsituTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;
    private static final int FILL_VALUE = Integer.MIN_VALUE;
    // package access for testing only tb 2014-03-13
    int[] historyShape;

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder
                .type(DATA_TYPE)
                .fillValue(FILL_VALUE)
                .unit(Constants.UNIT_INSITU_DTIME);
    }

    @Override
    public final Array apply(Array _, Item __) throws RuleException {
        final Context context = getContext();
        final ReferenceObservation refObs = context.getMatchup().getRefObs();
        final double referenceTime = TimeUtil.dateToSecondsSinceEpoch(refObs.getTime());
        final Reader observationReader = context.getObservationReader();

        if (observationReader != null) {
            try {
                final Configuration configuration = context.getConfiguration();
                final int halfExtractionTimeRangeInSeconds = configuration.getIntValue(
                        Configuration.KEY_MMS_SAMPLING_EXTRACTION_TIME);
                final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                        .shape(historyShape)
                        .halfExtractDuration(halfExtractionTimeRangeInSeconds)
                        .referenceObservation(refObs)
                        .build();
                final Array targetArray = observationReader.read("insitu.time", extractDefinition);
                final Item sourceColumn = observationReader.getColumn("insitu.time");
                final Number sourceFillValue = sourceColumn.getFillValue();
                for (int i = 0; i < targetArray.getSize(); i++) {
                    final int insituTime = targetArray.getInt(i);
                    if (sourceFillValue == null || sourceFillValue.intValue() != insituTime) {
                        targetArray.setDouble(i, insituTime - referenceTime);
                    } else {
                        targetArray.setInt(i, FILL_VALUE);
                    }
                }
                return targetArray;
            } catch (IOException e) {
                throw new RuleException("Unable to read in-situ time", e);
            }
        } else {
            final Array targetArray = Array.factory(DATA_TYPE, historyShape);
            for (int i = 0; i < targetArray.getSize(); i++) {
                targetArray.setInt(i, FILL_VALUE);
            }
            return targetArray;
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        historyShape = InsituHelper.getShape(context);
    }
}
