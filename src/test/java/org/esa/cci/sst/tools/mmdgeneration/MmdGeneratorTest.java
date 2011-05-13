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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.tools.BasicTool;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdGeneratorTest {

    private MmdGenerator mmdGenerator;

    @Before
    public void setUp() throws Exception {
        final BasicTool tool = new BasicTool("", "") {
        };
        tool.getConfiguration().setProperty("mmd.output.variables", "src/main/config/mmd-variables.properties");
        tool.getConfiguration().setProperty("mms.source.0.sensor", "atsr_md");
        tool.getConfiguration().setProperty("mms.source.0.reader", "GunzipDecorator,AtsrMdIOHandler");
        tool.getConfiguration().setProperty("mms.source.1.sensor", "metop");
        tool.getConfiguration().setProperty("mms.source.1.reader", "GunzipDecorator,MetopIOHandler");
        tool.getConfiguration().setProperty("mms.source.2.sensor", "seviri");
        tool.getConfiguration().setProperty("mms.source.2.reader", "GunzipDecorator,SeviriIOHandler");
        tool.getConfiguration().setProperty("mms.source.5.sensor", "atsr_orb.3");
        tool.getConfiguration().setProperty("mms.source.5.reader", "GunzipDecorator,ProductHandler");
        mmdGenerator = new MmdGenerator(tool);
    }

    @SuppressWarnings({"ReuseOfLocalVariable"})
    @Test
    public void testGetReaderSpecification() throws Exception {
        String readerSpec = mmdGenerator.getReaderSpec("atsr_md");
        assertEquals("GunzipDecorator,AtsrMdIOHandler", readerSpec);
        readerSpec = mmdGenerator.getReaderSpec("metop");
        assertEquals("GunzipDecorator,MetopIOHandler", readerSpec);
        readerSpec = mmdGenerator.getReaderSpec("seviri");
        assertEquals("GunzipDecorator,SeviriIOHandler", readerSpec);
        readerSpec = mmdGenerator.getReaderSpec("atsr_orb.3");
        assertEquals("GunzipDecorator,ProductHandler", readerSpec);
        readerSpec = mmdGenerator.getReaderSpec("mmd");
        assertNull(readerSpec);


    }
}
