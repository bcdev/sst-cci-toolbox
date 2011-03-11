/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.Variable;

/**
 * @author Thomas Storm
 */
public class ReaderUtils {

    static int[] createOriginArray(int matchupIndex, Variable variable) {
        String dimString = variable.getDimensions();
        final String dimensionRoles = variable.getDimensionRoles();
        String[] dims = dimString.split(" ");
        int length = dims.length;
        final boolean addMatchup = !(dimString.contains(Constants.DIMENSION_NAME_MATCHUP) ||
                                     dimensionRoles.contains(Constants.DIMENSION_NAME_MATCHUP));
        length += addMatchup ? 1 : 0;
        final int[] origin = new int[length];
        origin[0] = matchupIndex;
        for (int i = 1; i < origin.length; i++) {
            origin[i] = 0;
        }
        return origin;
    }
}
