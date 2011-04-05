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

package org.esa.cci.sst;

import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdIngestionToolTest {

    private MmdIngestionTool tool;

    @Before
    public void setUp() throws Exception {
        tool = new MmdIngestionTool();
    }

    @Test
    public void testLoadConfiguration() throws Exception {
        tool.loadArcProperties();
        final Properties arcProperties = tool.getArcProperties();
        assertEquals("mmd.nc", arcProperties.getProperty("mms.test.arc3.output.filename"));
    }

    @Test(expected = IOException.class)
    public void testCloseReader() throws Exception {
        final FileReader reader = new FileReader("pom.xml");
        tool.closeReader(reader);
        reader.ready();
    }
}
