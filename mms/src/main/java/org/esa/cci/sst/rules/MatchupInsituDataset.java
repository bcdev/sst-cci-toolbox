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

    private static final byte[] FLAG_VALUES = new byte[]{
            Constants.MATCHUP_INSITU_DATASET_DRIFTER,
            Constants.MATCHUP_INSITU_DATASET_MOORING,
            Constants.MATCHUP_INSITU_DATASET_SHIP,
            Constants.MATCHUP_INSITU_DATASET_GTMBA,
            Constants.MATCHUP_INSITU_DATASET_RADIOMETER,
            Constants.MATCHUP_INSITU_DATASET_ARGO,
            Constants.MATCHUP_INSITU_DATASET_DUMMY_SEA_ICE,
            Constants.MATCHUP_INSITU_DATASET_DUMMY_DIURNAL_VARIABILITY,
            Constants.MATCHUP_INSITU_DATASET_DUMMY_BC,
            Constants.MATCHUP_INSITU_DATASET_XBT,
            Constants.MATCHUP_INSITU_DATASET_MBT,
            Constants.MATCHUP_INSITU_DATASET_CTD,
            Constants.MATCHUP_INSITU_DATASET_ANIMAL,
            Constants.MATCHUP_INSITU_DATASET_BOTTLE
    };

    private static final String FLAG_MEANINGS = "drifter mooring ship gtmba radiometer argo dummy_sea_ice dummy_diurnal_variability dummy_bc xbt mbt ctd animal bottle";
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
