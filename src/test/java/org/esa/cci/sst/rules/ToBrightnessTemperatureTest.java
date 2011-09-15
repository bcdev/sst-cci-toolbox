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

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class ToBrightnessTemperatureTest extends AbstractRuleTest {

    @Override
    protected ColumnBuilder configureSourceColumn(ColumnBuilder columnBuilder) {
        return columnBuilder
                .standardName("somebrightnesstemperature")
                .longName("brightness temperature")
                .unit("K")
                .type(DataType.FLOAT)
                .fillValue(-1.e+30f);
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertEquals("K", targetColumn.getUnit());
        assertEquals(DataType.SHORT.name(), targetColumn.getType());
    }

    @Override
    public void testNumericConversion() throws RuleException {
        Array sourceArray = Array.factory(new float[]{-1e+30f, 2.945658e+07f, 283.15f});
        Array targetArray = getRule().apply(sourceArray, getSourceColumn());
        assertEquals(-32768, targetArray.getShort(0));
        assertEquals(-32768, targetArray.getShort(1));
        assertEquals(2315*5, targetArray.getShort(2));
    }
}
