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

package org.esa.cci.sst.util;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
@Ignore
public class MmdFormatGeneratorTest {

    private MmdFormatGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new MmdFormatGenerator();
    }

    @Test
    public void testCreateOriginArray() throws Exception {
        final org.esa.cci.sst.data.Variable variable = new org.esa.cci.sst.data.Variable();
        variable.setDimensions("ni nj");
        variable.setDimensionRoles("ni nj");
        int[] originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("match_up ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("time");
        variable.setDimensionRoles("time");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("match_up time");
        variable.setDimensionRoles("match_up time");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("n ni nj");
        originArray = generator.createOriginArray(0, variable);
        assertEquals(4, originArray.length);
    }
}
