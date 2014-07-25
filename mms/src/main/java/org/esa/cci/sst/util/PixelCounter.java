package org.esa.cci.sst.util;/*
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

import ucar.ma2.Array;

/**
 * For counting cloudy and otherwise irrelevant pixels.
 *
 * @author Ralf Quast
 */
public class PixelCounter {

    public int count(Array maskArray) {
        int count = 0;
        if (maskArray != null) {
            for (int i = 0; i < maskArray.getSize(); i++) {
                final byte value = maskArray.getByte(i);
                if (value != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
