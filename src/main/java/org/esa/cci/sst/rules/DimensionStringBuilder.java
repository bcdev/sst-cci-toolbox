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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used for dimension-modifying rules.
 *
 * @author Ralf Quast
 */
final class DimensionStringBuilder {

    private final List<String> dimensions;

    DimensionStringBuilder(String dimensionsString) {
        dimensions = new ArrayList<String>(7);
        dimensions.addAll(Arrays.asList(dimensionsString.split("\\s")));
    }

    DimensionStringBuilder replace(int i, String dimension) throws RuleException {
        if (i > dimensions.size()) {
            throw new RuleException(
                    MessageFormat.format("Expected {0} or more dimensions, but actual number of dimensions is {1}.",
                                         i, dimensions.size()));
        }
        if (i == dimensions.size()) {
            dimensions.add(i, dimension);
        } else {
            dimensions.set(i, dimension);
        }
        return this;
    }

    int getDimensionCount() {
        return dimensions.size();

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final String dimension : dimensions) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(dimension);
        }
        return sb.toString();
    }
}
