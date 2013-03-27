package org.esa.cci.sst.common.cellgrid;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import static org.junit.Assert.assertArrayEquals;

public class MaskTest {

    @Test
    public void testUnmask() throws Exception {
        final GridDef gridDef = GridDef.createGlobal(8, 4);

        final int B1 = 0x0001;
        final int B2 = 0x0002;
        final int B3 = 0x0004;
        final int B4 = 0x0008;
        final Array array = Array.factory(DataType.INT, new int[]{4, 8}, new int[]{
                B1 | B2, B1 | B3, B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2,
                B1 | B3, B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2,
                B1 | B4, B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2, B1 | B3,
                B2 | B1, B2 | B3, B2 | B4, B3 | B4, B1 | B2, B1 | B2, B1 | B3, B1 | B4,
        });

        final Grid sourceGrid = new ArrayGrid(gridDef, array, null, 1, 0);
        final Grid targetGrid = Mask.create(sourceGrid, 0x01);
        final int[] expected = new int[]{
                1, 1, 1, 1, 0, 0, 0, 1,
                1, 1, 1, 0, 0, 0, 1, 1,
                1, 1, 0, 0, 0, 1, 1, 1,
                1, 0, 0, 0, 1, 1, 1, 1,
        };
        final int[] actual = new int[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = targetGrid.getSampleInt(i % 8, i / 8);
        }
        assertArrayEquals(expected, actual);
    }

}
