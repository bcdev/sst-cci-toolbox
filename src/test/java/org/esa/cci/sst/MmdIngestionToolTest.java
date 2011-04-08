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

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.reader.MmdReaderTest;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MmdIngestionToolTest {

    private MmdIngestionTool tool;

    @Before
    public void setUp() throws Exception {
        tool = new MmdIngestionTool();
        tool.init(new String[]{"-csrc\\test\\config\\mms-config.properties"});
    }

    @Test
    public void testGetMatchupdsFromFile() throws Exception {
        final URL resource = getClass().getResource(MmdReaderTest.TEST_WITH_ACTUAL_DATA);
        final NetcdfFile file = NetcdfFile.open(resource.getFile());
        final Variable matchupVariable = file.findVariable(NetcdfFile.escapeName("matchup_id"));
        final int[] origin = {0};
        final int[] shape = {10};
        final int[] ids = tool.readMatchupIdsFromFile(matchupVariable, origin, shape);
        for (int i = 0; i < ids.length; i++) {
            assertEquals(8368401 + i, ids[i]);
        }
    }

    @Test
    public void testGetMatchupAndObservation() throws Exception {
        tool.getObservations(7943562);
    }

    @Test
    public void testGetDataBaseObjectById() throws Exception {
        final Object observation = tool.getDatabaseObjectById(MmdIngestionTool.GET_OBSERVATION, 7545306);
        assertTrue(observation instanceof Observation);

        final Object matchup = tool.getDatabaseObjectById(MmdIngestionTool.GET_MATCHUP, 7976381);
        assertTrue(matchup instanceof Matchup);
    }
}
