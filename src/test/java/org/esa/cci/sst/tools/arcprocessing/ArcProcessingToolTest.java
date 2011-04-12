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

import org.esa.beam.framework.datamodel.GeoPos;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ArcProcessingToolTest {

    private ArcProcessingTool arcProcessingTool;

    @Before
    public void setUp() throws Exception {
        arcProcessingTool = new ArcProcessingTool();
        arcProcessingTool.setCommandLineArgs(new String[]{"-csrc/test/config/mms-config.properties"});
        arcProcessingTool.initialize();
    }

    @Test
    public void testBlah() throws Exception {
        final List<GeoPos> coordinates = arcProcessingTool.getCoordinates();
        assertNotNull(coordinates);
        for (GeoPos coordinate : coordinates) {
            assertTrue(coordinate.isValid());
        }
    }
}
