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
 * Sensor pattern
 *
 * @author Ralf Quast
 */
@SuppressWarnings({"UnusedDeclaration"})
final class MatchupPattern extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.INT;
    private static final int[] SHAPE = new int[]{1};

    private static final int[] FLAG_MASKS = new int[]{
            1,
            1 << 1,
            1 << 2,
            1 << 3,
            1 << 4,
            1 << 5,
            1 << 6,
            1 << 7,
            1 << 8,
            1 << 9,
            1 << 10,
            1 << 11,
            1 << 12,
            1 << 13,
            1 << 14,
            1 << 15,
            1 << 16,
            1 << 17,
            1 << 18,
            1 << 19,
            1 << 20,
            1 << 21,
    };
    private static final String FLAG_MEANINGS =
            "amsre " +
            "amsr2 " +
            "tmi " +
            "atsr_1 " +
            "atsr_2 " +
            "atsr_3 " +
            "avhrr_noaa_tn " +
            "avhrr_noaa_06 " +
            "avhrr_noaa_07 " +
            "avhrr_noaa_08 " +
            "avhrr_noaa_09 " +
            "avhrr_noaa_10 " +
            "avhrr_noaa_11 " +
            "avhrr_noaa_12 " +
            "avhrr_noaa_13 " +
            "avhrr_noaa_14 " +
            "avhrr_noaa_15 " +
            "avhrr_noaa_16 " +
            "avhrr_noaa_17 " +
            "avhrr_noaa_18 " +
            "avhrr_noaa_19 " +
            "avhrr_metop_02 ";

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
        final Array targetArray = Array.factory(DATA_TYPE, SHAPE);
        targetArray.setLong(0, getContext().getMatchup().getPattern() & 0xFFFFFFFFL);
        return targetArray;
    }
}
