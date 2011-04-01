package org.esa.cci.sst.util;

/**
 * A source of sample values indexed by (x, y) with x in {0, ..., width - 1} and y
 * in {0, ..., height - 1}.
 * <p/>
 * This interface is an abstraction of any construct where samples values are arranged
 * on a rectangular raster.
 *
 * @author Ralf Quast
 */
public interface SampleSource {

    /**
     * Returns the raster width of the sample source.
     *
     * @return the raster width of the sample source.
     */
    int getWidth();

    /**
     * Returns the raster height of the sample source.
     *
     * @return the raster height of the sample source.
     */
    int getHeight();

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
