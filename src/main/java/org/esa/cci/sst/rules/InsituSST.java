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
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.InsituSource;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the longitude of the insitu observation, if present.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class InsituSST extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.FLOAT;
    private static final int[] SHAPE = {1, 1};
    private static final int[] SINGLE_VALUE_SHAPE = {1, 1};

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
                        .shape(SHAPE)
                        .referenceObservation(referenceObservation)
                        .build();
                return observationReader.read("insitu.sea_surface_temperature", extractDefinition);
            } else {
                final Array array = Array.factory(DATA_TYPE, SINGLE_VALUE_SHAPE);
                final InsituSource insituSource = context.getReferenceObservationReader().getInsituSource();
                if (insituSource != null) {
                    final double sst = insituSource.readInsituSst(referenceObservation.getRecordNo());
                    array.setDouble(0, sst);
                }
                return array;
            }
        } catch (IOException e) {
            throw new RuleException("Unable to read in-situ sea surface temperature", e);
        }
    }
}
