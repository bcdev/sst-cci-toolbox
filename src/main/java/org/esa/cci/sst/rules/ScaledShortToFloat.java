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
 * Rule for converting a scaled short into a float.
 *
 * @author Thomas Storm
 */
public class ScaledShortToFloat implements Rule {

    @Override
    public VariableDescriptor apply(VariableDescriptor sourceDescriptor) throws RuleException {
        RuleUtil.ensureType(DataType.SHORT.name(), sourceDescriptor.getType());
        final VariableDescriptor targetDescriptor = new VariableDescriptor(sourceDescriptor);
        targetDescriptor.setScaleFactor(null);
        targetDescriptor.setAddOffset(null);
        targetDescriptor.setType(DataType.FLOAT.name());
        return targetDescriptor;
    }

    @Override
    public Number apply(Number number, VariableDescriptor targetDescriptor,
                        VariableDescriptor sourceDescriptor) throws RuleException {
        final Number addOffset = sourceDescriptor.getAddOffset();
        final Number scaleFactor = sourceDescriptor.getScaleFactor();
        float result = number.floatValue();
        if (scaleFactor != null) {
            result *= scaleFactor.floatValue();
        }
        if (addOffset != null) {
            result += addOffset.floatValue();
        }
        return result;
    }
}
