package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GbcsToolTest {

    @Test
    public void testInputFilename_ATSR1() throws Exception {
        assertEquals("MMD_ATSR1.inp", GbcsTool.getInputFilename("atsr.1"));

    }

    @Test
    public void testInputFilename_ATSR2() throws Exception {
        assertEquals("MMD_ATSR2.inp", GbcsTool.getInputFilename("atsr.2"));

    }

    @Test
    public void testInputFilename_ATSR3() throws Exception {
        assertEquals("MMD_AATSR.inp", GbcsTool.getInputFilename("atsr.3"));
    }

    @Test
    public void testInputFilename_ForIllegalSensor() throws Exception {
        try {
            GbcsTool.getInputFilename("something else flying around");
            fail();
        } catch (ToolException e) {
            assertEquals(ToolException.TOOL_CONFIGURATION_ERROR, e.getExitCode());
        }
    }
}
