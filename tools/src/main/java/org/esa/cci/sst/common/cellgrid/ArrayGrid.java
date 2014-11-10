/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.cellgrid;

import org.esa.cci.sst.common.GridDef;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * A grid that is backed by an array instance.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class ArrayGrid implements Grid {

    private final GridDef gridDef;
    private final Array array;
    private final double scaling;
    private final double offset;
    private final FillTest fillTest;
    private final int width;
    private final int height;

    public static Grid create(GridDef gridDef, float[] data) {
        return new ArrayGrid(gridDef,
                             Array.factory(DataType.FLOAT, new int[]{gridDef.getHeight(), gridDef.getWidth()}, data),
                             Float.NaN, 1.0, 0.0);
    }

    public static ArrayGrid create(GridDef gridDef, double[] data) {
        return new ArrayGrid(gridDef,
                             Array.factory(DataType.DOUBLE, new int[]{gridDef.getHeight(), gridDef.getWidth()}, data),
                             Double.NaN, 1.0, 0.0);
    }

    public ArrayGrid(GridDef gridDef, Array array, final Number fillValue, double scaling, double offset) {
        this.gridDef = gridDef;
        this.array = array;
        this.scaling = scaling;
        this.offset = offset;
        this.fillTest = createFillTest(fillValue);
        width = gridDef.getWidth();
        height = gridDef.getHeight();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getScaling() {
        return scaling;
    }

    public double getOffset() {
        return offset;
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new ArrayIndexOutOfBoundsException(
                    "width: " + width + "; height: " + height + "; x = " + x + "; y = " + y);
        }
        final int index = y * width + x;
        return array.getBoolean(index);
    }

    @Override
    public int getSampleInt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new ArrayIndexOutOfBoundsException(
                    "width: " + width + "; height: " + height + "; x = " + x + "; y = " + y);
        }
        final int index = y * width + x;
        return array.getInt(index);
    }

    @Override
    public double getSampleDouble(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new ArrayIndexOutOfBoundsException(
                    "width: " + width + "; height: " + height + "; x = " + x + "; y = " + y);
        }

        final int index = y * width + x;
        final double sample = array.getDouble(index);
        if (fillTest.isFill(sample)) {
            return Double.NaN;
        }
        return scaling * sample + offset;
    }

    public void setSample(int x, int y, double sample) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new ArrayIndexOutOfBoundsException(
                    "width: " + width + "; height: " + height + "; x = " + x + "; y = " + y);
        }
        final int index = y * width + x;
        array.setDouble(index, sample);
    }

    private interface FillTest {

        boolean isFill(double sample);
    }

    private static FillTest createFillTest(Number fillValue) {

        if (fillValue == null) {
            return new FillTest() {
                @Override
                public boolean isFill(double sample) {
                    return false;
                }
            };
        } else {
            final double fillSample = fillValue.doubleValue();
            if (Double.isNaN(fillSample)) {
                return new FillTest() {
                    @Override
                    public boolean isFill(double sample) {
                        return Double.isNaN(sample);
                    }
                };
            } else {
                return new FillTest() {
                    @Override
                    public boolean isFill(double sample) {
                        return sample == fillSample;
                    }
                };
            }
        }
    }
}
