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

package org.esa.cci.sst.tools.nwp;

import org.junit.Test;
import ucar.ma2.Array;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Storm
 */
public class NwpToolTest {

    @Test
    public void testGetSensorBasename() throws Exception {
        assertEquals("atsr", NwpTool.getSensorBasename("atsr.1"));
        assertEquals("atsr", NwpTool.getSensorBasename("atsr.2"));
        assertEquals("atsr", NwpTool.getSensorBasename("atsr.3"));

        assertEquals("avhrr", NwpTool.getSensorBasename("avhrr.n14"));
        assertEquals("avhrr", NwpTool.getSensorBasename("avhrr.m02"));
    }

    @Test
    public void testComputeFutureTimeStepCount() throws Exception {
        assertEquals(12, NwpTool.computeFutureTimeStepCount(33));
        assertEquals(6, NwpTool.computeFutureTimeStepCount(17));
    }

    @Test
    public void testComputePastTimeStepCount() throws Exception {
        assertEquals(20, NwpTool.computePastTimeStepCount(33));
        assertEquals(10, NwpTool.computePastTimeStepCount(17));
    }

    @Test
    public void testGetMatchupCount_noDataInArray() {
        final Array array = mock(Array.class);

        when(array.getSize()).thenReturn(0L);

        assertEquals(0, NwpTool.getMatchupCount(array, 876));
    }

    @Test
    public void testGetMatchupCount() {
        final Array array = mock(Array.class);

        when(array.getSize()).thenReturn(3L);
        when(array.getInt(0)).thenReturn(16);
        when(array.getInt(1)).thenReturn(9);
        when(array.getInt(2)).thenReturn(18);

        assertEquals(2, NwpTool.getMatchupCount(array, 16));
    }
}
