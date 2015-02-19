/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tool.ToolException;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class IngestionToolTest {

    @Test
    public void testCommandLineArgs() throws URISyntaxException {
        IngestionTool noArgs = new IngestionTool();
        assertTrue(noArgs.setCommandLineArgs(new String[]{}));
        // @todo fails on windows tb 2015.02.18
//        if (new File(BasicTool.DEFAULT_CONFIGURATION_FILE_NAME).exists()) {
//            assertNotNull(noArgs.getConfig().getStringValue("openjpa.ConnectionURL"));
//        }

        IngestionTool configOnly = new IngestionTool();
        final String filePath = TestHelper.getResourcePath(getClass(), "ingestionToolTest.properties");
        final File configFile = new File(filePath);
        assertTrue(configOnly.setCommandLineArgs(new String[]{"-c", configFile.getPath()}));
        assertEquals("value1", configOnly.getConfig().getStringValue("mms.name1"));
        if (new File(BasicTool.DEFAULT_CONFIGURATION_FILE_NAME).exists()) {
            try {
                configOnly.getConfig().getStringValue("openjpa.ConnectionURL");
                fail("ToolException expected");
            } catch (ToolException expected) {
                //
            }
        }

        IngestionTool printHelp = new IngestionTool();
        assertFalse(printHelp.setCommandLineArgs(new String[]{"-help"}));
    }

    @Test
    public void testConfiguration() {
        System.setProperty("mms.someParam", "someValue");

        try {
            IngestionTool ingestionTool = new IngestionTool();
            assertEquals("someValue", ingestionTool.getConfig().getStringValue("mms.someParam"));
        } finally {
            System.clearProperty("mms.someParam");
        }
    }
}
