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
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class JulianDateToSecondsShortTest {

    private JulianDateToSecondsShort rule;
    private Item sourceItem;

    @Before
    public void setUp() throws Exception {
        rule = new JulianDateToSecondsShort();
        sourceItem = new ColumnBuilder()
                .fillValue(100000.0)
                .type(DataType.DOUBLE)
                .unit("Julian Date")
                .build();
    }

    @Test
    public void testApply() throws Exception {
        Item targetItem = rule.apply(sourceItem);
        assertEquals((short) -202479220800.0, targetItem.getFillValue());
    }

    @Test
    public void testApplyAndConvert() throws Exception {
        Array values = Array.factory(new double[]{56346346.4});
        Array targetArray = rule.apply(values, sourceItem);
        assertEquals((short) 4657205108160.0, targetArray.getShort(0));
    }
}
