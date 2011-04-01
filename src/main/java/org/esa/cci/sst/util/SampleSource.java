package org.esa.cci.sst.util;

/**
 * A source of sample values indexed by (x, y) with x in {0, ..., maxX} and y
 * in {0, ..., maxY}.
 * <p/>
 * This interface is an abstraction of any construct where samples values are
 * arranged on a rectangular raster.
 *
 * @author Ralf Quast
 */
public interface SampleSource {

    /**
     * Returns the maximum value of x.
     *
     * @return the maximum value of x.
     */
    int getMaxX();

    /**
     * Returns the maximum value of y.
     *
     * @return the maximum value of y.
     */
    int getMaxY();

    /**
     * Returns the value of the sample indexed by (x, y).
     *
     * @param x The value of the x index.
     * @param y The value of the y index.
     *
     * @return the sample value at (x, y).
     */
    double getSample(int x, int y);
}
