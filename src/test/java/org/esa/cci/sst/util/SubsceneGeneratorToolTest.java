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

import org.junit.*;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SubsceneGeneratorToolTest {

    @Test
    public void testGetFilenames() throws Exception {
        final URL resource = getClass().getResource("generate_subscenes.csv");
        final String[] filenames = SubsceneGeneratorTool.getFilenames(resource.getFile());
        assertEquals(5, filenames.length);
        assertEquals("testdata/ATSR_Level1b/ATS_TOA_1PRUPA20100601_011802_000065272090_00002_43142_4494.N1", filenames[0]);
        assertEquals("testfile.N1", filenames[1]);
        assertEquals("dilbert", filenames[2]);
        assertEquals("jeff", filenames[3]);
        assertEquals("wally", filenames[4]);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test
    public void testGetSubsceneGenerator() throws Exception {
        final SubsceneGeneratorTool.SubsceneGenerator generator = SubsceneGeneratorTool.getSubsceneGenerator(
                "testdata/ATSR_Level1b/ATS_TOA_1PRUPA20100601_011802_000065272090_00002_43142_4494.N1");
        assertTrue(generator instanceof ProductSubsceneGenerator);
        assertFalse(generator instanceof NetcdfSubsceneGenerator);
    }
}
