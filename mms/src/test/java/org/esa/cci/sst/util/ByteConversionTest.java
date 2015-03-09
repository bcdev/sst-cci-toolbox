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

package org.esa.cci.sst.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class ByteConversionTest {

    @Test
    public void testBytesToInt() throws Exception {
        final int[] ints = ByteConversion.bytesToInts(new byte[]{0x01, 0x02, 0x03, 0x04});

        assertEquals(0x01020304, ints[0]);
    }

    @Test
    public void testIntsToBytes() throws Exception {
        final byte[] bytes = ByteConversion.intsToBytes(new int[]{0x01020304});

        assertEquals(0x01, bytes[0]);
        assertEquals(0x02, bytes[1]);
        assertEquals(0x03, bytes[2]);
        assertEquals(0x04, bytes[3]);
    }

}
