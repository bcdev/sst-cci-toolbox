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

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.image.Raster;

/**
 * A simple facade wrapping a {@link RasterDataNode}.
 *
 * @author Ralf Quast
 */
public class RasterDataNodeSampleSource implements SampleSource {

    private final RasterDataNode node;
    private final Raster data;

    /**
     * Construct a new instance of this class.
     *
     * @param node The raster data node to be wrapped.
     */
    public RasterDataNodeSampleSource(RasterDataNode node) {
        this.node = node;
        this.data = node.getSourceImage().getImage(0).getData();
    }

    /**
     * Returns the scene raster width of the wrapped node.
     *
     * @return the scene raster width of the wrapped node.
     */
    @Override
    public int getWidth() {
        return node.getSceneRasterWidth();
    }

    /**
     * Returns the scene raster height of the wrapped node.
     *
     * @return the scene raster height of the wrapped node.
     */
    @Override
    public int getHeight() {
        return node.getSceneRasterHeight();
    }

    @Override
    public double getSample(int x, int y) {
        final double sample;
        final int dataType = node.getDataType();
        if (dataType == ProductData.TYPE_INT8) {
            sample = (byte) data.getSample(x, y, 0);
        } else if (dataType == ProductData.TYPE_UINT32) {
            sample = data.getSample(x, y, 0) & 0xFFFFFFFFL;
        } else {
            sample = data.getSampleDouble(x, y, 0);
        }
        if (node.isScalingApplied()) {
            return node.scale(sample);
        }
        return sample;
    }

}
