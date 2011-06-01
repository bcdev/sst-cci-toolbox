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

package org.esa.cci.sst.rules;

import org.junit.Test;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class LandSeaMaskTest {

    @Test
    public void testName() throws Exception {
        int recordNo = 0;
        int[] shape = new int[]{0, 2, 6};

        int pixelX;
        int pixelY;
        for (int i = 0; i < 12; i++) {
            pixelX = i % shape[1] + (recordNo * shape[1] * shape[2]);
            pixelY = i % shape[2] + (recordNo * shape[1] * shape[2]);

            System.out.println("pixelX = " + pixelX);
            System.out.println("pixelY = " + pixelY);
            System.out.println();
        }
    }
}
