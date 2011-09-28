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
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * Matchup insitu dataset.
 *
 * @author Ralf Quast
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class MatchupInsituDataset extends AbstractImplicitRule {

    private static final byte[] FLAG_VALUES = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final String FLAG_MEANINGS = "drifter mooring ship gtmba radiometer argo dummy_sea_ice dummy_diurnal_variability";
    private static final DataType DATA_TYPE = DataType.BYTE;

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        targetColumnBuilder.type(DATA_TYPE).
                unsigned(true).
                rank(1).
                dimensions(Constants.DIMENSION_NAME_MATCHUP).
                flagValues(FLAG_VALUES).
                flagMeanings(FLAG_MEANINGS);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final byte dataset = getContext().getMatchup().getRefObs().getDataset();
        final Array array = Array.factory(DATA_TYPE, new int[]{1});
        array.setByte(0, dataset);
        return array;
    }
}
