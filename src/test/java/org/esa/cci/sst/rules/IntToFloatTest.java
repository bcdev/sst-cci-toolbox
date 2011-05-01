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

import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.DescriptorBuilder;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class IntToFloatTest extends AbstractRuleTest {

    @Override
    @Test
    public void testNumericConversion() throws RuleException {
        final Rule rule = getRule();
        final Descriptor sourceDescriptor = getSourceDescriptor();
        final Number result = rule.apply(5, sourceDescriptor);

        assertTrue(result instanceof Float);
        assertEquals(10.5f, result.floatValue(), 0.0f);
    }

    @Test(expected = RuleException.class)
    public void testDescriptorConversion_ImproperType() throws RuleException {
        getRule().apply(new DescriptorBuilder().setType(DataType.BYTE).build());
    }

    @Override
    public void assertTargetDescriptor(Descriptor targetDescriptor) {
        assertTrue(DataType.FLOAT.name().equals(targetDescriptor.getType()));
        assertTrue(targetDescriptor.getAddOffset() == null);
        assertTrue(targetDescriptor.getScaleFactor() == null);
        assertTrue(targetDescriptor.getFillValue() instanceof Float);
        assertTrue(targetDescriptor.getFillValue().floatValue() == -1.5f);
    }

    @Override
    protected DescriptorBuilder configureSourceDescriptorBuilder(DescriptorBuilder descriptorBuilder) {
        descriptorBuilder.setType(DataType.INT);
        descriptorBuilder.setAddOffset(0.5f);
        descriptorBuilder.setScaleFactor(2.0f);
        descriptorBuilder.setFillValue(-1);

        return descriptorBuilder;
    }

}
