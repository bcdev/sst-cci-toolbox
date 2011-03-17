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

package org.esa.cci.sst.subscene;

import org.esa.cci.sst.orm.PersistenceManager;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Storm
 */
public class SubsceneGeneratorToolTest {

    private URL resource;
    private URL wrongResource;

    @Before
    public void setUp() throws Exception {
        resource = getClass().getResource("generate_subscenes.csv");
        wrongResource = getClass().getResource("generate_subscenes_wrong.csv");
    }

    @Test
    public void testGetFilenames() throws Exception {
        SubsceneGeneratorTool.SubsceneIO[] subsceneIOs = SubsceneGeneratorTool.getFilenames(resource.getFile());
        assertEquals(5, subsceneIOs.length);
        assertEquals("testdata/ATSR_Level1b/ATS_TOA_1PRUPA20100603_051636_000065272090_00033_43173_4544.N1",
                     subsceneIOs[0].getInputFilename());
        assertEquals("testdata/subscenes/ATSR_Level1b/test_subscene.nc", subsceneIOs[0].getOutputFilename());
        assertEquals("testfile.N1", subsceneIOs[1].getInputFilename());
        assertEquals("testfile_subscene.N1", subsceneIOs[1].getOutputFilename());
        assertEquals("dilbert", subsceneIOs[2].getInputFilename());
        assertEquals("dilbert_subscene", subsceneIOs[2].getOutputFilename());
        assertEquals("jeff", subsceneIOs[3].getInputFilename());
        assertEquals("jeff_subscene", subsceneIOs[3].getOutputFilename());
        assertEquals("wally", subsceneIOs[4].getInputFilename());
        assertEquals("wally_subscene", subsceneIOs[4].getOutputFilename());
    }

    @Test
    public void testGetFilenamesWrong() throws Exception {
        try {
            SubsceneGeneratorTool.getFilenames(wrongResource.getFile());
            fail();
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test
    public void testGetSubsceneGenerator() throws Exception {
        PersistenceManager manager = mock(PersistenceManager.class);
        SubsceneGeneratorTool.SubsceneGenerator generator = SubsceneGeneratorTool.getSubsceneGenerator(
                "testdata/ATSR_Level1b/ATS_TOA_1PRUPA20100603_051636_000065272090_00033_43173_4544.N1", manager);
        assertTrue(generator instanceof ProductSubsceneGenerator);
        assertFalse(generator instanceof NetcdfSubsceneGenerator);

        try {
            SubsceneGeneratorTool.getSubsceneGenerator(
                    "testdata/ATSR_Level1b/ATS_TOA_1PRUPA20100603_051636_000065272090_00033_43173_4544.N1", null);
            fail();
        } catch (IllegalArgumentException expected) {
            System.out.println("expected.getMessage() = " + expected.getMessage());
        }
        // todo - ts - perhaps not even needed
//        generator = SubsceneGeneratorTool.getSubsceneGenerator(
//                "testdata/AMSRE/20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42957.dat-v01.nc");
//        assertFalse(generator instanceof ProductSubsceneGenerator);
//        assertTrue(generator instanceof NetcdfSubsceneGenerator);
//
//        generator = SubsceneGeneratorTool.getSubsceneGenerator(
//                "testdata/TMI/20100601-TMI-REMSS-L2P-tmi_L2b_v04_071449.dat-v01.nc");
//        assertFalse(generator instanceof ProductSubsceneGenerator);
//        assertTrue(generator instanceof NetcdfSubsceneGenerator);

    }

    @Test
    public void testCreateDefaultSubsceneName() throws Exception {
        assertEquals("abc_subscene", SubsceneGeneratorTool.createDefaultSubsceneName("abc"));
        assertEquals("abc_def_subscene", SubsceneGeneratorTool.createDefaultSubsceneName("abc_def"));
        assertEquals("abc_subscene.N1", SubsceneGeneratorTool.createDefaultSubsceneName("abc.N1"));
    }
}
