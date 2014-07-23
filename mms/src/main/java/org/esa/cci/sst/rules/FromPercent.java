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

/**
 * Applicable to reflectance measured in percent.
 *
 * @author Ralf Quast
 */
final class FromPercent extends AbstractAttributeModification {

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        Assert.condition(sourceColumn.getAddOffset() == null || sourceColumn.getAddOffset().doubleValue() == 0.0,
                         "expected addOffset == null or addOffset == 0.0");

        final Number scaleFactor = sourceColumn.getScaleFactor();
        if (scaleFactor == null) {
            targetColumnBuilder.scaleFactor(0.01);
        } else {
            targetColumnBuilder.scaleFactor(scaleFactor.doubleValue() * 0.01);
        }
        targetColumnBuilder.unit("1");
    }
}
