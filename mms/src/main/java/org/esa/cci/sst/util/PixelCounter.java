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
 * For counting cloudy (and otherwise invalid) pixels.
 *
 * @author Ralf Quast
 */
public class PixelCounter {

    private final int mask;
    private final Number fillValue;

    public PixelCounter(int mask, Number fillValue) {
        this.mask = mask;
        this.fillValue = fillValue;
    }

    public int count(Array array) {
        int count = 0;
        for (int i = 0; i < array.getSize(); i++) {
            final int value = array.getInt(i);
            if (fillValue != null && value == fillValue.intValue()) {
                count++;
            } else if ((value & mask) != 0) {
                count++;
            }
        }
        return count;
    }
}
