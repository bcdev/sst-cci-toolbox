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

public class CloudyPixelCounter {

    private final int cloudFlag;
    private final Number fillValue;

    public CloudyPixelCounter(int cloudFlag, Number fillValue) {
        this.cloudFlag = cloudFlag;
        this.fillValue = fillValue;
    }

    public int count(Array array) {
        int count = 0;
        for (int i = 0; i < array.getSize(); i++) {
            final int value = array.getInt(i);
            if ((fillValue == null || value != fillValue.intValue()) && (value & cloudFlag) == cloudFlag) {
                count++;
            }
        }
        return count;
    }
}
