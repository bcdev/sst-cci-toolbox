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
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class ShortToFloatTest extends AbstractRuleTest {

    @Override
    @Test
    public void testNumericConversion() throws RuleException {
        final Rule rule = getRule();
        final VariableDescriptor sourceDescriptor = getSourceDescriptor();
        final Number result = rule.apply((short) 5, sourceDescriptor);

        assertTrue(result instanceof Float);
        assertEquals(10.5f, result.floatValue(), 0.0f);
    }

    @Test(expected = RuleException.class)
    public void testDescriptorConversion_ImproperType() throws Exception {
        final VariableDescriptor sourceDescriptor = getSourceDescriptor();
        sourceDescriptor.setType(DataType.BYTE.name());

        getRule().apply(sourceDescriptor);
    }

    @Override
    protected void assertTargetDescriptor(VariableDescriptor targetDescriptor) {
        assertTrue(targetDescriptor.getScaleFactor() == null);
        assertTrue(targetDescriptor.getAddOffset() == null);
        assertTrue(DataType.FLOAT.name().equals(targetDescriptor.getType()));
        assertTrue(targetDescriptor.getFillValue() instanceof Float);
        assertTrue(targetDescriptor.getFillValue().floatValue() == -1.5f);
    }

    @Override
    protected void configureSourceDescriptor() {
        final VariableDescriptor sourceDescriptor = getSourceDescriptor();
        sourceDescriptor.setType(DataType.SHORT.name());
        sourceDescriptor.setAddOffset(0.5f);
        sourceDescriptor.setScaleFactor(2.0f);
        sourceDescriptor.setFillValue(-1);
    }

}
