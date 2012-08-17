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
 * A {@link Grid} that is backed by a {@code ucar.ma2.Array} (from NetCDF library) instance.
 *
 * @author Norman Fomferra
 */
public class ArrayGrid implements Grid {
    private String variable;
    private final GridDef gridDef;
    private final Array array;
    private final double scaling;
    private final double offset;
    private final Number fillValue;
    private final FillTest fillTest;
    private final int width;
    private final int height;

    public static ArrayGrid createWith2DDoubleArray(GridDef gridDef) {
        return createWith2DDoubleArray(gridDef, null);
    }

    public static ArrayGrid createWith2DDoubleArray(GridDef gridDef, double[] data) {
        return new ArrayGrid(gridDef, Array.factory(DataType.DOUBLE, new int[] {gridDef.getHeight(), gridDef.getWidth()}, data), Double.NaN, 1.0, 0.0);
    }

    public ArrayGrid(GridDef gridDef, Array array, final Number fillValue, double scaling, double offset) {
        this.gridDef = gridDef;
        this.array = array;
        this.scaling = scaling;
        this.offset = offset;
        this.fillValue = fillValue;
        this.fillTest = createFillTest(fillValue);
        width = gridDef.getWidth();
        height = gridDef.getHeight();
    }

    public String getVariable() {
        return variable;
    }

    public ArrayGrid setVariable(String variable) {
        if (this.variable != null) {
            throw new RuntimeException("Variable is allowed to be set only once.");
        }
        this.variable = variable;
        return this;
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

    public void setSample(int x, int y, boolean sample) {
        int index = y * width + x;
        array.setBoolean(index, sample);
    }

    @Override
    public int getSampleInt(int x, int y) {
        int index = y * width + x;
        return array.getInt(index);
    }

    public void setSample(int x, int y, int sample) {
        int index = y * width + x;
        array.setInt(index, sample);
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

    public void setSample(int x, int y, double sample) {
        int index = y * width + x;
        array.setDouble(index, sample);
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
        final ArrayGrid newArrayGrid = new ArrayGrid(newGridDef, newArray, null, 1.0, 0.0);
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
        return new ArrayGrid(gridDef, newArray, null, 1.0, 0.0);
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
