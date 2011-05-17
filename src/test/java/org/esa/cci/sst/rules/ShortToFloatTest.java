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
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class ShortToFloatTest extends AbstractRuleTest {

    @Override
    protected ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder) {
        columnBuilder.type(DataType.SHORT);
        columnBuilder.addOffset(0.5f);
        columnBuilder.scaleFactor(2.0f);
        columnBuilder.fillValue((short) -1);

        return columnBuilder;
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertTrue(targetColumn.getScaleFactor() == null);
        assertTrue(targetColumn.getAddOffset() == null);
        assertTrue(DataType.FLOAT.name().equals(targetColumn.getType()));
        assertTrue(targetColumn.getFillValue() instanceof Float);
        assertTrue(targetColumn.getFillValue().floatValue() == -1.5f);
    }

    @Override
    @Test
    public void testNumericConversion() throws RuleException {
        final Array sourceArray = Array.factory(DataType.SHORT, new int[]{2});
        sourceArray.setShort(0, (short) 5);
        sourceArray.setShort(1, (short) 7);

        final Array targetArray = getRule().apply(sourceArray, getSourceColumn());

        assertTrue(targetArray.getElementType() == float.class);
        assertEquals(10.5f, targetArray.getFloat(0), 0.0f);
        assertEquals(14.5f, targetArray.getFloat(1), 0.0f);
    }

    @Test(expected = RuleException.class)
    public void testColumnConversion_ImproperType() throws Exception {
        getRule().apply(new ColumnBuilder().type(DataType.BYTE).build());
    }

}
