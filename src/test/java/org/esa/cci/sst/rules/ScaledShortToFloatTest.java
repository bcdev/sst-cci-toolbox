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
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ScaledShortToFloatTest {

    private ScaledShortToFloat rule;

    @Before
    public void setUp() throws Exception {
        rule = new ScaledShortToFloat();
    }

    @Test
    public void testCreateTargetDescriptor() throws Exception {
        final VariableDescriptor source = new VariableDescriptor();
        final VariableDescriptor target = createTargetDescriptor(source);

        assertTrue(target.getScaleFactor() == null);
        assertTrue(target.getAddOffset() == null);
        assertTrue(target.getType().equals(DataType.FLOAT.name()));
    }

    @Test(expected = RuleException.class)
    public void testCreateTargetDescriptor_Fail() throws Exception {
        final VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.setType(DataType.BYTE.name());
        rule.apply(descriptor);
    }

    @Test
    public void testScaling() throws Exception {
        final VariableDescriptor source = new VariableDescriptor();
        final VariableDescriptor target = createTargetDescriptor(source);
        final Number result = rule.apply(5, target, source);
        assertEquals(10.5, result.floatValue(), 0.0);
        assertTrue(result instanceof Float);
    }

    private VariableDescriptor createTargetDescriptor(final VariableDescriptor source) throws RuleException {
        source.setType(DataType.SHORT.name());
        source.setAddOffset(0.5);
        source.setScaleFactor(2.0);
        return rule.apply(source);
    }
}
