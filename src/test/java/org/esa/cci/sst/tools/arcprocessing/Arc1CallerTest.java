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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.tools.MmsTool;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class Arc1CallerTest {

    private Arc1Caller arc1Caller;

    @Before
    public void setUp() throws Exception {
        final MmsTool tool = new MmsTool("name", "version");
        tool.setCommandLineArgs(new String[]{"-csrc/test/config/mms-config.properties"});
        tool.initialize();
        arc1Caller = new Arc1Caller(tool);
    }

    @Test
    public void testGetAvhrrFiles() throws Exception {
        final List<String> paths = arc1Caller.getAvhrrFilePaths();
        assertNotNull(paths);
        for (String path : paths) {
            assertTrue(path.contains("AVHRR_GAC"));
            assertTrue(path.contains("noaa-"));
            assertTrue(path.contains("NSS.GHRR."));
            assertTrue(path.endsWith("GC") || path.endsWith("WI") || path.endsWith("SV"));
        }
    }

}
