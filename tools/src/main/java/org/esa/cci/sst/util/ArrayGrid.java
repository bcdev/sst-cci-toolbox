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

package org.esa.cci.sst.util;

import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * A {@link Grid} that is backed by a NetCDF {@code Array} instance.
 *
 * @author Norman Fomferra
 */
public class ArrayGrid implements Grid {
    private final GridDef gridDef;
    private final Array array;
    private final double scaling;
    private final double offset;
    private final Number fillValue;
    private final FillTest fillTest;
    private final int width;
    private final int height;

    public ArrayGrid(GridDef gridDef, double scaling, double offset, final Number fillValue, Array array) {
        this.gridDef = gridDef;
        this.array = array;
        this.scaling = scaling;
        this.offset = offset;
        this.fillValue = fillValue;
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

    public Number getFillValue() {
        return fillValue;
    }

    public Array getArray() {
        return array;
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        int index = y * width + x;
        return array.getBoolean(index);
    }

    @Override
    public int getSampleInt(int x, int y) {
        int index = y * width + x;
        return array.getInt(index);
    }

    @Override
    public double getSampleDouble(int x, int y) {
        int index = y * width + x;
        double sample = array.getDouble(index);
        if (fillTest.isFill(sample)) {
            return Double.NaN;
        }
        return scaling * sample + offset;
    }

    public ArrayGrid scaleDown(int scaleX, int scaleY) {
        final GridDef newGridDef = new GridDef(gridDef.getWidth() / scaleX,
                                               gridDef.getHeight() / scaleY,
                                               gridDef.getEasting(),
                                               gridDef.getNorthing(),
                                               gridDef.getResolutionX() * scaleX,
                                               gridDef.getResolutionY() * scaleY);
        Class elementType = array.getElementType();
        Class newElementType;
        if (Double.TYPE.equals(elementType)) {
            newElementType = Double.TYPE;
        } else {
            newElementType = Float.TYPE;
        }
        final int newWidth = newGridDef.getWidth();
        final int newHeight = newGridDef.getHeight();
        final Array newArray = Array.factory(newElementType, new int[]{newHeight, newWidth});
        final ArrayGrid newArrayGrid = new ArrayGrid(newGridDef, 1.0, 0.0, null, newArray);
        for (int yd = 0; yd < newHeight; yd++) {
            for (int xd = 0; xd < newWidth; xd++) {
                double sum = 0.0;
                int n = 0;
                for (int dy = 0; dy < scaleY; dy++) {
                    final int ys = yd * scaleY + dy;
                    for (int dx = 0; dx < scaleX; dx++) {
                        final int xs = xd * scaleX + dx;
                        final double sample = getSampleDouble(xs, ys);
                        if (!Double.isNaN(sample)) {
                            sum += sample;
                            n++;
                        }
                    }
                }
                newArrayGrid.setSample(xd, yd, n > 0 ? sum / n : Double.NaN);
            }
        }
        return newArrayGrid;
    }

    public ArrayGrid unmask(int mask) {
        final Array newArray = Array.factory(DataType.BYTE, new int[]{height, width});
        for (int i = 0; i < width * height; i++) {
            newArray.setByte(i, (array.getInt(i) & mask) != 0 ? (byte) 1 : (byte) 0);
        }
        return new ArrayGrid(gridDef, 1.0, 0.0, null, newArray);
    }

    private void setSample(int x, int y, double sample) {
        int index = y * width + x;
        array.setDouble(index, sample);
    }

    public void flipY() {
        final int yOff = height - 1;
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x++) {
                int i1 = y * width + x;
                int i2 = (yOff - y) * width + x;
                double sample1 = array.getDouble(i1);
                double sample2 = array.getDouble(i2);
                array.setDouble(i1, sample2);
                array.setDouble(i2, sample1);
             }
        }
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
