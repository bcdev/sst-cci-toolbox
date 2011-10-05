package org.esa.cci.sst.util;

import ucar.ma2.Array;

/**
 * todo - add api doc
 *
 * @author Norman Fomferra
 */
public class ArrayGrid implements Grid {
    final GridDef gridDef;
    final Array data;
    final double scaling;
    final double offset;
    final Number fillValue;
    final FillTest fillTest;

    public ArrayGrid(GridDef gridDef, double scaling, double offset, final Number fillValue, Array data) {
        this.gridDef = gridDef;
        this.data = data;
        this.scaling = scaling;
        this.offset = offset;
        this.fillValue = fillValue;
        this.fillTest = createFillTest(fillValue);
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        int index = y * gridDef.getWidth() + x;
        return data.getBoolean(index);
    }

    @Override
    public int getSampleInt(int x, int y) {
        int index = y * gridDef.getWidth() + x;
        return data.getInt(index);
    }

    @Override
    public double getSampleDouble(int x, int y) {
        int index = y * gridDef.getWidth() + x;
        double sample = data.getDouble(index);
        if (fillTest.isFill(sample)) {
            return Double.NaN;
        }
        return scaling * sample + offset;
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

    public ArrayGrid resample(GridDef targetGridDef) {
        int sourceWidth = gridDef.getWidth();
        int sourceHeight = gridDef.getHeight();
        int targetWidth = targetGridDef.getWidth();
        int targetHeight = targetGridDef.getHeight();
        System.out.printf("Resampling from %d x %d --> %d x %d (NOT IMPLEMENTED YET!)...\n", sourceWidth, sourceHeight, targetWidth, targetHeight);
        // todo implement resampling by averaging
        return this;
    }

}
