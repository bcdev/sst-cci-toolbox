/*
 * Copyright (C) 2011-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NwpUtilTest {

    private Variable variable;
    private Array array;

    @Before
    public void setUp() throws IOException {
        variable = mock(Variable.class);
        final Attribute fillValueAttribute = mock(Attribute.class);
        when(fillValueAttribute.getNumericValue()).thenReturn(Integer.MIN_VALUE);

        array = mock(Array.class);

        when(variable.findAttribute("_FillValue")).thenReturn(fillValueAttribute);
        when(variable.read()).thenReturn(array);
    }

    @Test
    public void testGetRelevantNwpDirs_noTimeInfo() throws IOException {
        final List<String> relevantNwpDirs = NwpUtil.getRelevantNwpDirs(variable, null);
        assertNotNull(relevantNwpDirs);
        assertEquals(0, relevantNwpDirs.size());
    }

    @Test
    public void testGetRelevantNwpDirs_oneMeasurement() throws IOException {
        when(array.getSize()).thenReturn(1L);
        when(array.getInt(0)).thenReturn(118302044);

        final List<String> relevantNwpDirs = NwpUtil.getRelevantNwpDirs(variable, null);
        assertNotNull(relevantNwpDirs);
        assertEquals(6, relevantNwpDirs.size());
        assertEquals("1981/09/28", relevantNwpDirs.get(0));
        assertEquals("1981/10/01", relevantNwpDirs.get(3));
        assertEquals("1981/10/03", relevantNwpDirs.get(5));
    }

    @Test
    public void testGetRelevantNwpDirs_threeMeasurements() throws IOException {
        when(array.getSize()).thenReturn(3L);
        when(array.getInt(0)).thenReturn(176093689);
        when(array.getInt(1)).thenReturn(176094298);
        when(array.getInt(2)).thenReturn(176098678);

        final List<String> relevantNwpDirs = NwpUtil.getRelevantNwpDirs(variable, null);
        assertNotNull(relevantNwpDirs);
        assertEquals(6, relevantNwpDirs.size());
        assertEquals("1983/07/29", relevantNwpDirs.get(0));
        assertEquals("1983/08/01", relevantNwpDirs.get(3));
        assertEquals("1983/08/03", relevantNwpDirs.get(5));
    }

    @Test
    public void testGetRelevantNwpDirs_threeMeasurements_plusInvalid() throws IOException {
        when(array.getSize()).thenReturn(4L);
        when(array.getInt(0)).thenReturn(220945222);
        when(array.getInt(1)).thenReturn(220975771);
        when(array.getInt(2)).thenReturn(Integer.MIN_VALUE);
        when(array.getInt(3)).thenReturn(220992459);

        final List<String> relevantNwpDirs = NwpUtil.getRelevantNwpDirs(variable, null);
        assertNotNull(relevantNwpDirs);
        assertEquals(6, relevantNwpDirs.size());
        assertEquals("1984/12/29", relevantNwpDirs.get(0));
        assertEquals("1985/01/01", relevantNwpDirs.get(3));
        assertEquals("1985/01/03", relevantNwpDirs.get(5));
    }

    @Test
    public void testGetRelevantNwpDirs_threeMeasurements_onlyFillValue() throws IOException {
        when(array.getSize()).thenReturn(3L);
        when(array.getInt(0)).thenReturn(Integer.MIN_VALUE);
        when(array.getInt(1)).thenReturn(Integer.MIN_VALUE);
        when(array.getInt(2)).thenReturn(Integer.MIN_VALUE);

        final List<String> relevantNwpDirs = NwpUtil.getRelevantNwpDirs(variable, null);
        assertNotNull(relevantNwpDirs);
        assertEquals(0, relevantNwpDirs.size());
    }

    @Test
    public void testNearestTimeStep_oneTimeValue() {
        when(array.getSize()).thenReturn(1L);
        when(array.getInt(0)).thenReturn(15);

        final int nearestTimeStep = NwpUtil.nearestTimeStep(array, 18);
        assertEquals(0, nearestTimeStep);
    }

    @Test
    public void testNearestTimeStep_twoTimeValues() {
        when(array.getSize()).thenReturn(2L);
        when(array.getInt(0)).thenReturn(15);
        when(array.getInt(1)).thenReturn(19);

        final int nearestTimeStep = NwpUtil.nearestTimeStep(array, 18);
        assertEquals(1, nearestTimeStep);
    }

    @Test
    public void testNearestTimeStep_twoTimeValues_invertedOrder() {
        when(array.getSize()).thenReturn(2L);
        when(array.getInt(0)).thenReturn(19);
        when(array.getInt(1)).thenReturn(15);

        final int nearestTimeStep = NwpUtil.nearestTimeStep(array, 18);
        assertEquals(0, nearestTimeStep);
    }

    @Test
    public void testNearestTimeStep_threeTimeValues() {
        when(array.getSize()).thenReturn(3L);
        when(array.getInt(0)).thenReturn(21);
        when(array.getInt(1)).thenReturn(17);
        when(array.getInt(2)).thenReturn(13);

        final int nearestTimeStep = NwpUtil.nearestTimeStep(array, 18);
        assertEquals(1, nearestTimeStep);
    }
}
