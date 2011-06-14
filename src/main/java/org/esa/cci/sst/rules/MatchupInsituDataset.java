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
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Matchup insitu dataset.
 *
 * @author Ralf Quast
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class MatchupInsituDataset extends AbstractImplicitRule {

    private static final byte[] FLAG_MASKS = new byte[]{1, 2, 4, 8, 16, 32, 64};
    private static final String FLAG_MEANINGS = "drifter moored ship gtmba radiometer argo dummy";
    private static final DataType DATA_TYPE = DataType.BYTE;

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        targetColumnBuilder.type(DATA_TYPE).
                unsigned(true).
                rank(1).
                dimensions(Constants.DIMENSION_NAME_MATCHUP).
                flagMasks(FLAG_MASKS).
                flagMeanings(FLAG_MEANINGS);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DATA_TYPE, new int[]{1});
        array.setByte(0, readInsituDataset());
        return array;
    }

    private byte readInsituDataset() throws RuleException {
        final Context context = getContext();
        final ReferenceObservation referenceObservation = context.getMatchup().getRefObs();
        final Reader reader = context.getReferenceObservationReader();
        final String sensor = referenceObservation.getSensor();
        String variableName;
        if ("atsr_md".equalsIgnoreCase(sensor)) {
            variableName = "insitu.dataset";
        } else if ("metop".equalsIgnoreCase(sensor) || "seviri".equalsIgnoreCase(sensor)) {
            variableName = "msr_type";
        } else {
            throw new IllegalStateException(MessageFormat.format("Illegal primary sensor: ''{0}''.", sensor));
        }

        Array value;
        try {
            value = reader.read(variableName, new OneDimOneValue(referenceObservation.getRecordNo()));
        } catch (IOException e) {
            throw new RuleException("Unable to read from variable '" + variableName + "'.", e);
        }
        return value.getByte(0);
    }

}
