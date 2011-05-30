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

package org.esa.cci.sst.reader;

import org.junit.Test;
import ucar.ma2.Array;

import java.awt.Point;

import static org.junit.Assert.*;

public class MdReaderTest {

    @Test
    public void testExtractMdSubscene() {
        final int[][][] sourceArray = new int[1][21][21];
        final int[][][] targetArray = new int[1][15][15];

        for (int y = 0; y < sourceArray[0].length; y++) {
            for (int x = 0; x < sourceArray[0][y].length; x++) {
                sourceArray[0][y][x] = y * 21 + x + 1;
            }
        }
        final Array source = Array.factory(sourceArray);
        final Array target = Array.factory(targetArray);


        MdReader.extractSubscene(source, target, new Point(2, 2), -1);

        for (int y = 0; y < target.getShape()[1]; y++) {
            assertEquals(-1, target.getInt(target.getIndex().set(0, y, 0)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, y, 1)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, y, 2)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, y, 3)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, y, 4)));
            if (y < 5) {
                assertEquals(-1, target.getInt(target.getIndex().set(0, y, 5)));
            }
        }
        for (int x = 0; x < target.getShape()[2]; x++) {
            assertEquals(-1, target.getInt(target.getIndex().set(0, 0, x)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, 1, x)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, 2, x)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, 3, x)));
            assertEquals(-1, target.getInt(target.getIndex().set(0, 4, x)));
            if (x < 5) {
                assertEquals(-1, target.getInt(target.getIndex().set(0, 5, x)));
            }
        }

        assertEquals(1, target.getInt(target.getIndex().set(0, 5, 5)));
        assertEquals(2, target.getInt(target.getIndex().set(0, 5, 6)));
        assertEquals(22, target.getInt(target.getIndex().set(0, 6, 5)));
        assertEquals(9 * 21 + 9 + 1, target.getInt(target.getIndex().set(0, 14, 14)));
    }

}
