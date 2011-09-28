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

package org.esa.beam.util;

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

    Number getFillValue();
}
