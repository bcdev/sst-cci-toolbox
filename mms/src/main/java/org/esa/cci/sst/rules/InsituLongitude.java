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
import org.esa.cci.sst.util.GeometryUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the longitude of the insitu observation, if present. Otherwise, the reference observation's longitude is set.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class InsituLongitude extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.FLOAT;
    private static final int[] SINGLE_VALUE_SHAPE = {1, 1};
    // package access for testing only tb 2014-03-13
    int[] historyShape = {1, 96};

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws
                                                                                                     RuleException {
        targetColumnBuilder.type(DATA_TYPE);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Context context = getContext();
        final ReferenceObservation referenceObservation = context.getMatchup().getRefObs();
        final Reader observationReader = context.getObservationReader();
        try {
            if (observationReader != null) {
                final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                        .shape(historyShape)
                        .referenceObservation(referenceObservation)
                        .build();
                final Array targetArray = observationReader.read("insitu.lon", extractDefinition);
                for (int i = 0; i < targetArray.getSize(); i++) {
                    targetArray.setDouble(i, GeometryUtil.normalizeLongitude(targetArray.getDouble(i)));
                }
                return targetArray;
            } else {
                final Array array = Array.factory(DATA_TYPE, SINGLE_VALUE_SHAPE);
                final InsituSource insituSource = context.getReferenceObservationReader().getInsituSource();
                if (insituSource != null) {
                    final double lon = insituSource.readInsituLon(referenceObservation.getRecordNo());
                    array.setDouble(0, lon);
                }
                return array;
            }
        } catch (IOException e) {
            throw new RuleException("Unable to read in-situ longitude", e);
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        historyShape = InsituHelper.getShape(context);
    }
}
