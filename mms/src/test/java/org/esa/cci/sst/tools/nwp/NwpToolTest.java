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

import static org.junit.Assert.assertEquals;
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
    public void testGetMatchupCount_noInputData() {
        final Array array = mock(Array.class);
        when(array.getSize()).thenReturn(0L);

        final int matchupCount = NwpTool.getMatchupCount(array, 800);
        assertEquals(0, matchupCount);
    }

    @Test
    public void testGetMatchupCount_noMatchingPattern() {
        final Array array = mock(Array.class);
        when(array.getSize()).thenReturn(3L);
        when(array.getInt(0)).thenReturn(200);
        when(array.getInt(1)).thenReturn(1600);
        when(array.getInt(2)).thenReturn(400);

        final int matchupCount = NwpTool.getMatchupCount(array, 800);
        assertEquals(0, matchupCount);
    }

    @Test
    public void testGetMatchupCount_matchingPattern() {
        final Array array = mock(Array.class);
        when(array.getSize()).thenReturn(4L);
        when(array.getInt(0)).thenReturn(400);
        when(array.getInt(1)).thenReturn(1600);
        when(array.getInt(2)).thenReturn(800);
        when(array.getInt(3)).thenReturn(8);

        final int matchupCount = NwpTool.getMatchupCount(array, 800);
        assertEquals(1, matchupCount);
    }

    @Test
    public void testCalculateStride() {
        assertEquals(1, NwpTool.calculateStride(108, 0));
        assertEquals(1, NwpTool.calculateStride(108, 1));

        assertEquals(107, NwpTool.calculateStride(108, 2));
        assertEquals(53, NwpTool.calculateStride(108, 3));

        assertEquals(200, NwpTool.calculateStride(201, 2));
        assertEquals(149, NwpTool.calculateStride(300, 3));
    }
}
