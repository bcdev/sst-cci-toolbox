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
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.File;

/**
 * Matchup filename.
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
final class MatchupPrimaryFilename extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.CHAR;
    private static final int[] SHAPE = new int[]{1, 80};

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array targetArray = Array.factory(DATA_TYPE, SHAPE);
        final String filePath = getContext().getMatchup().getRefObs().getDatafile().getPath();
        final String filename = new File(filePath).getName();
        for (int i = 0; i < Math.min(SHAPE[1], filename.length()); i++) {
            targetArray.setChar(i, filename.charAt(i));
        }
        return targetArray;
    }
}
