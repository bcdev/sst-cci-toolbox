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
import org.esa.cci.sst.data.ColumnI;
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
        final ColumnI sourceColumn = getSourceColumn();
        final Number result = rule.apply(5, sourceColumn);

        assertTrue(result instanceof Float);
        assertEquals(10.5f, result.floatValue(), 0.0f);
    }

    @Test(expected = RuleException.class)
    public void testColumnConversion_ImproperType() throws RuleException {
        getRule().apply(new ColumnBuilder().setType(DataType.BYTE).build());
    }

    @Override
    protected void assertTargetColumn(ColumnI targetColumn) {
        assertTrue(DataType.FLOAT.name().equals(targetColumn.getType()));
        assertTrue(targetColumn.getAddOffset() == null);
        assertTrue(targetColumn.getScaleFactor() == null);
        assertTrue(targetColumn.getFillValue() instanceof Float);
        assertTrue(targetColumn.getFillValue().floatValue() == -1.5f);
    }

    @Override
    protected ColumnBuilder configureSourceColumnBuilder(ColumnBuilder columnBuilder) {
        columnBuilder.setType(DataType.INT);
        columnBuilder.setAddOffset(0.5f);
        columnBuilder.setScaleFactor(2.0f);
        columnBuilder.setFillValue(-1);

        return columnBuilder;
    }

}
