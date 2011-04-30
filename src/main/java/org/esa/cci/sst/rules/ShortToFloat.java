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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.VariableDescriptor;
import ucar.ma2.DataType;

/**
 * Rule for converting type 'SHORT' into 'FLOAT'.
 *
 * @author Thomas Storm
 */
final class ShortToFloat implements Rule {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        Assert.type(DataType.SHORT.name(), sourceDescriptor);

        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setType(DataType.FLOAT.name());
        targetDescriptor.setAddOffset(null);
        targetDescriptor.setScaleFactor(null);
        final Number sourceFillValue = sourceDescriptor.getFillValue();
        if (sourceFillValue != null) {
            targetDescriptor.setFillValue(apply(sourceFillValue.shortValue(), sourceDescriptor));
        }

        return targetDescriptor;
    }

    @Override
    public Float apply(Number number, VariableDescriptor sourceDescriptor) throws RuleException {
        Assert.condition(number instanceof Short, "number instanceof Short");

        Number addOffset = sourceDescriptor.getAddOffset();
        Number scaleFactor = sourceDescriptor.getScaleFactor();
        if (scaleFactor == null) {
            scaleFactor = 1.0f;
        }
        if (addOffset == null) {
            addOffset = 0.0f;
        }

        return number.floatValue() * scaleFactor.floatValue() + addOffset.floatValue();
    }
}
