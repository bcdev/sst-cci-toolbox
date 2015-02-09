/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.metop;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class InternalTargetTemperatureReaderTest {

    @Test
    public void testComputeThermometerIndices_1() throws Exception {
        final short[] counts = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(0, indices[0]);
        assertEquals(1, indices[1]);
        assertEquals(2, indices[2]);
        assertEquals(3, indices[3]);
        assertEquals(-1, indices[4]);
        assertEquals(0, indices[5]);
    }

    @Test
    public void testComputeThermometerIndices_2() throws Exception {
        final short[] counts = {1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(1, indices[0]);
        assertEquals(2, indices[1]);
        assertEquals(3, indices[2]);
        assertEquals(-1, indices[3]);
        assertEquals(0, indices[4]);
        assertEquals(1, indices[5]);
    }

    @Test
    public void testComputeThermometerIndices_3() throws Exception {
        final short[] counts = {1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(2, indices[0]);
        assertEquals(3, indices[1]);
        assertEquals(-1, indices[2]);
        assertEquals(0, indices[3]);
        assertEquals(1, indices[4]);
        assertEquals(2, indices[5]);
    }

    @Test
    public void testComputeThermometerIndices_4() throws Exception {
        final short[] counts = {1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(3, indices[0]);
        assertEquals(-1, indices[1]);
        assertEquals(0, indices[2]);
        assertEquals(1, indices[3]);
        assertEquals(2, indices[4]);
        assertEquals(3, indices[5]);
    }

    @Test
    public void testComputeThermometerIndices_5() throws Exception {
        final short[] counts = {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(-1, indices[0]);
        assertEquals(0, indices[1]);
        assertEquals(1, indices[2]);
        assertEquals(2, indices[3]);
        assertEquals(3, indices[4]);
        assertEquals(-1, indices[5]);
    }

    @Test
    public void testComputeThermometerIndices_withManyZeros() throws Exception {
        final short[] counts = {1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1};
        final int[] indices = InternalTargetTemperatureBandReader.getThermometerIndices(counts);

        assertEquals(6, indices.length);

        assertEquals(3, indices[0]);
        assertEquals(-1, indices[1]);
        assertEquals(0, indices[2]);
        assertEquals(1, indices[3]);
        assertEquals(2, indices[4]);
        assertEquals(3, indices[5]);
    }
}
