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

/**
 * Replaces the second and third dimension with 'metop.ny' and 'metop.nx', respectively.
 *
 * @author Ralf Quast
 */
final class MetopImageDimensions extends AbstractDimensionReplacement {

    @Override
    protected void replaceDimensions(DimensionStringBuilder builder) throws RuleException {
        builder.replace(1, "metop.ny");
        builder.replace(2, "metop.nx");
    }
}
