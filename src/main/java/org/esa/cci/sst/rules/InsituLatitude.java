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
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Sets the latitude of the insitu observation, if present. Otherwise, the reference observation's latitude is set.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class InsituLatitude extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.FLOAT;
    private static final int[] SHAPE = new int[]{1, 48};

    @Override
    protected final void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws
                                                                                                     RuleException {
        targetColumnBuilder.type(DATA_TYPE);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Context context = getContext();
        final Reader observationReader = context.getObservationReader();
        if (observationReader != null) {
            final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                    .shape(SHAPE)
                    .referenceObservation(context.getMatchup().getRefObs())
                    .build();
            try {
                return observationReader.read("insitu.longitude", extractDefinition);
            } catch (IOException e) {
                throw new RuleException("Unable to read longitude", e);
            }
        } else {
            final Array array = Array.factory(DATA_TYPE, SHAPE);
            final ReferenceObservation referenceObservation = getContext().getMatchup().getRefObs();
            final double lon = referenceObservation.getPoint().getGeometry().getFirstPoint().getX();
            for(int i = 0; i < array.getSize(); i++) {
                array.setFloat(i, (float) lon);
            }
            return array;
        }
    }
}
