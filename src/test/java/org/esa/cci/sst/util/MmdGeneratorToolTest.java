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

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdGeneratorToolTest {

    @Test
    public void testGetSelectedVariables() throws Exception {
        final URL resource = getClass().getResource("vars.csv");
        assertTrue(MmdGeneratorTool.isOutputVariable("sally.frog_density", resource.getFile()));
        assertFalse(MmdGeneratorTool.isOutputVariable("sally.flog_density", resource.getFile()));

        assertTrue(MmdGeneratorTool.isOutputVariable("wally.frog_density", resource.getFile()));
        assertTrue(MmdGeneratorTool.isOutputVariable("wally.dog_density", resource.getFile()));

        assertTrue(MmdGeneratorTool.isOutputVariable("kally.dog_density", resource.getFile()));
        assertTrue(MmdGeneratorTool.isOutputVariable("kally.bog_density", resource.getFile()));
        assertTrue(MmdGeneratorTool.isOutputVariable("kally.og_density", resource.getFile()));

        assertFalse(MmdGeneratorTool.isOutputVariable("kally.frog_density", resource.getFile()));

        assertTrue(MmdGeneratorTool.isOutputVariable("kally.latitude", resource.getFile()));
        assertTrue(MmdGeneratorTool.isOutputVariable("wally.latitude", resource.getFile()));
        assertTrue(MmdGeneratorTool.isOutputVariable("sally.latitude", resource.getFile()));
    }
}
