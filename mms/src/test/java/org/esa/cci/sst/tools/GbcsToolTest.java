/*
 * Copyright (C) 2012-2016 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.tool.ToolException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GbcsToolTest {

    @Test
    public void testInputFilename_ATSR() throws Exception {
        assertEquals("MMD_ATSR1.inp", GbcsTool.getConfigurationFilename("atsr.1"));
        assertEquals("MMD_ATSR2.inp", GbcsTool.getConfigurationFilename("atsr.2"));
        assertEquals("MMD_AATSR.inp", GbcsTool.getConfigurationFilename("atsr.3"));
    }

    @Test
    public void testInputFilename_AVHRR_Metop() throws Exception {
        assertEquals("MMD_METOP02.inp", GbcsTool.getConfigurationFilename("avhrr.m02"));
    }

    @Test
    public void testInputFilename_AVHRR_NOAA() throws Exception {
        assertEquals("MMD_NOAA19.inp", GbcsTool.getConfigurationFilename("avhrr.n19"));
        assertEquals("MMD_NOAA16.inp", GbcsTool.getConfigurationFilename("avhrr.n16"));
        assertEquals("MMD_NOAA09.inp", GbcsTool.getConfigurationFilename("avhrr.n09"));
        assertEquals("MMD_NOAA08.inp", GbcsTool.getConfigurationFilename("avhrr.n08"));
        assertEquals("MMD_NOAA07.inp", GbcsTool.getConfigurationFilename("avhrr.n07"));
        assertEquals("MMD_NOAA06.inp", GbcsTool.getConfigurationFilename("avhrr.n06"));
    }

    @Test
    public void testInputFilename_AVHRR_FRAC() throws Exception {
        assertEquals("MMD_FRAC01.inp", GbcsTool.getConfigurationFilename("avhrr_f.m01"));
        assertEquals("MMD_FRAC02.inp", GbcsTool.getConfigurationFilename("avhrr_f.m02"));
    }

    @Test
    public void testInputFilename_ForIllegalSensor() throws Exception {
        try {
            GbcsTool.getConfigurationFilename("something else flying around");
            fail();
        } catch (ToolException e) {
            assertEquals(ToolException.TOOL_CONFIGURATION_ERROR, e.getExitCode());
        }
    }
}
