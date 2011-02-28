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

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

import static org.junit.Assert.*;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class MmdFormatGeneratorTest {

    @Test
    public void testAddContent() throws Exception {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew("");
        file.addDimension("matchup", 0, false, true, true);
        final String var1 = "var1";
        file.addVariable(var1, DataType.LONG, "matchup");
        generator.addContent(file);
        assertTrue(file.findVariable(var1).read().getSize() > 0);
    }
}
