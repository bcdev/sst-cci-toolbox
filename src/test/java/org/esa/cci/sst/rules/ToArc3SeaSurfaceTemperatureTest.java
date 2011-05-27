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
import org.esa.cci.sst.tools.Constants;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ToArc3SeaSurfaceTemperatureTest {

    @Test
    public void testApply() throws Exception {
        ToArc3SeaSurfaceTemperature rule = new ToArc3SeaSurfaceTemperature();
        Item sourceItem = new ColumnBuilder().build();
        Item targetItem = rule.apply(sourceItem);
        assertEquals(Constants.UNIT_SEA_SURFACE_TEMPERATURE, targetItem.getUnit());
    }
}
