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

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class FloatPercentToShortTest {

    private FloatPercentToShort floatPercentToShort;

    @Before
    public void setUp() throws Exception {
        floatPercentToShort = new FloatPercentToShort();
    }

    @Test
    public void testApply() throws Exception {
        final Array sourceArray = Array.factory(new float[]{12.3f, 18.67f, 29.894f, 57.12345f});
        final Array targetArray = Array.factory(short.class, sourceArray.getShape());
        floatPercentToShort.apply(sourceArray, targetArray, null, null);
        assertEquals(1230, targetArray.getShort(0));
        assertEquals(1867, targetArray.getShort(1));
        assertEquals(2989, targetArray.getShort(2));
        assertEquals(5712, targetArray.getShort(3));
    }
}
