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

/**
 * Used for dimension replacing rules.
 *
 * @author Ralf Quast
 */
final class DimensionReplacer {

    private final String[] dimensions;

    DimensionReplacer(String dimensionsString) {
        dimensions = dimensionsString.split("\\s+");
    }

    DimensionReplacer replace(int i, String dimension) throws RuleException {
        if (dimensions.length < i + 1) {
            throw new RuleException(
                    MessageFormat.format("Expected {0} or more dimensions, but actual number of dimensions is {1}.",
                                         i + 1,
                                         dimensions.length));
        }
        dimensions[i] = dimension;
        return this;
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
