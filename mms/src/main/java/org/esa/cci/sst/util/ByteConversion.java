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

/**
* @author Ralf Quast
*/
public class ByteConversion {

    private ByteConversion() {
    }

    public static int[] bytesToInts(byte[] bytes) {
        final int[] ints = new int[bytes.length >> 2];
        for (int i = 0; i < ints.length; ++i) {
            final byte b4 = bytes[i * 4];
            final byte b3 = bytes[i * 4 + 1];
            final byte b2 = bytes[i * 4 + 2];
            final byte b1 = bytes[i * 4 + 3];
            ints[i] = (b1 & 0xFF) | (b2 & 0xFF) << 8 | (b3 & 0xFF) << 16 | (b4 & 0xFF) << 24;
        }
        return ints;
    }

    public static byte[] intsToBytes(int[] ints) {
        final byte[] bytes = new byte[ints.length << 2];
        for (int i = 0; i < ints.length; ++i) {
            final int value = ints[i];
            bytes[i * 4] = (byte) (value >> 24 & 0xFF);
            bytes[i * 4 + 1] = (byte) (value >> 16 & 0xFF);
            bytes[i * 4 + 2] = (byte) (value >> 8 & 0xFF);
            bytes[i * 4 + 3] = (byte) (value & 0xFF);
        }
        return bytes;
    }

}
