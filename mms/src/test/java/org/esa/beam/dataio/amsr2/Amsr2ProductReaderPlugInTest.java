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

package org.esa.beam.dataio.amsr2;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Amsr2ProductReaderPlugInTest {

    @Test
    public void testCanDecode_nominalFile() throws Exception {
        final String filename = "GW1AM2_201503142222_158A_L1SGRTBR_1110110.h5";

        assertTrue(Amsr2ProductReaderPlugIn.isCorrectFilename(filename));
    }

    @Test
    public void testCanDecode_unpackedTemporaryFile() throws Exception {
        final String filename = "GW1AM2_201503142222_158A_L1SGRTBR_1110110666496307406491968.h5";

        assertTrue(Amsr2ProductReaderPlugIn.isCorrectFilename(filename));
    }
}
