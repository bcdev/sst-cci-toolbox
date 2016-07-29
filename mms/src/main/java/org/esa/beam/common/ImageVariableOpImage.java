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

package org.esa.beam.common;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.VariableIF;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Used for creating rendered images from an image variable in
 * netCDF files.
 *
 * @author Ralf Quast
 */
public abstract class ImageVariableOpImage extends SingleBandedOpImage {

    protected final VariableIF variable;

    protected ImageVariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                                   Dimension tileSize, ResolutionLevel level) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        final int[] stride = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
            stride[i] = 1;
        }
        final int indexX = getIndexX(rank);
        final int indexY = getIndexY(rank);

        shape[indexX] = getSourceShapeX(rectangle.width);
        shape[indexY] = getSourceShapeY(rectangle.height);

        origin[indexX] = getSourceOriginX(rectangle.x) + getSourceOriginX();
        origin[indexY] = getSourceOriginY(rectangle.y) + getSourceOriginY();

        final double scale = getScale();
        stride[indexX] = (int) scale * getSourceStrideX();
        stride[indexY] = (int) scale * getSourceStrideY();

        Array array;
        synchronized (variable.getParentGroup().getNetcdfFile()) {
            try {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            } catch (IOException | InvalidRangeException e) {
                throw new RuntimeException(e);
            }
        }
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(array));
    }

    protected int getSourceOriginX(int x) {
        return getSourceX(x);
    }

    protected int getSourceOriginY(int y) {
        return getSourceY(y);
    }

    protected int getSourceShapeX(int width) {
        return getSourceWidth(width);
    }

    protected int getSourceShapeY(int height) {
        return getSourceHeight(height);
    }

    /**
     * Returns the index of the x dimension of the variable, which
     * provides the image data.
     *
     * @param rank The rank of the array, which contains the image
     *             data.
     *
     * @return the index of the x dimension.
     */
    protected abstract int getIndexX(int rank);

    /**
     * Returns the index of the y dimension of the variable, which
     * provides the image data.
     *
     * @param rank The rank of the array, which contains the image
     *             data.
     *
     * @return the index of the y dimension.
     */
    protected abstract int getIndexY(int rank);

    /**
     * Returns the origin of the x dimension of the variable, which
     * provides the image data.
     *
     * @return the origin of the x dimension.
     */
    protected int getSourceOriginX() {
        return 0;
    }

    /**
     * Returns the origin of the y dimension of the variable, which
     * provides the image data.
     *
     * @return the origin of the y dimension.
     */
    protected int getSourceOriginY() {
        return 0;
    }

    protected int getSourceStrideY() {
        return 1;
    }

    protected int getSourceStrideX() {
        return 1;
    }

    /**
     * Transforms the primitive storage of the array supplied as argument.
     * <p/>
     * The default implementation merely returns the primitive storage of
     * the array supplied as argument, which is fine when the sequence of
     * variable dimensions is (..., y, x).
     * <p/>
     * Implementations have to transpose the storage when the sequence of
     * variable dimensions is (..., x, y) instead of (..., y, x).
     * <p/>
     *
     * @param array An array.
     *
     * @return the transformed primitive storage of the array supplied as
     * argument.
     */
    protected Object transformStorage(Array array) {
        return array.getStorage();
    }

}
