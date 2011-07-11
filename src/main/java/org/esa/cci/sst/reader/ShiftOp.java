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

package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;

/**
 * Allows to shift the images of a product.
 *
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "Shift", internal = true)
public class ShiftOp extends Operator {

    @SourceProduct(alias = "source", description = "The product which images are to be shifted.",
                   label = "Name")
    private Product sourceProduct;

    @Parameter(
            description = "Specifies the naming pattern of the bands to shift. If no pattern is specified, all bands are shifted.",
            notNull = false)
    private String bandNamesPattern;

    @Parameter(description = "Specifies the shift in x direction.", label = "Shift X", notNull = true)
    private int shiftX;

    @Parameter(description = "Specifies the shift in y direction.", label = "Shift Y", notNull = true)
    private int shiftY;

    @Parameter(description = "Specifies the fill value.", label = "Fill value", notNull = true)
    private Number fillValue;

    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        validateParameters();
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), sceneWidth, sceneHeight);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        for (String bandName : sourceProduct.getBandNames()) {
            ProductUtils.copyBand(bandName, sourceProduct, targetProduct);
            final Band band = targetProduct.getBand(bandName);
            if (!bandName.matches(bandNamesPattern)) {
                band.setSourceImage(sourceProduct.getBand(bandName).getSourceImage());
            } else {
                band.setNoDataValue(fillValue.doubleValue());
            }
        }
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setStartTime(sourceProduct.getStartTime());
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Band sourceBand = sourceProduct.getBand(targetBand.getName());
        if (!targetBand.getName().matches(bandNamesPattern)) {
            return;
        }

        final Rectangle rectangle = computeSourceRect(targetTile);
        final Tile sourceTile = getSourceTile(sourceBand, rectangle);
        final ProductData rawSamples = sourceTile.getRawSamples();
        for (Tile.Pos pos : targetTile) {
            int targetX = pos.x;
            int targetY = pos.y;
            final int xPos = pos.x - shiftX;
            final int yPos = pos.y - shiftY;
            if (isPositionOutOfBounds(xPos, yPos)) {
                targetTile.setSample(pos.x, pos.y, fillValue.doubleValue());
                continue;
            }
            final int sample = rawSamples.getElemIntAt(sourceTile.getWidth() * yPos + xPos);
            targetTile.setSample(targetX, targetY, sample);
        }
    }

    @Override
    public void dispose() {
        if (sourceProduct != null) {
            sourceProduct.dispose();
        }
        super.dispose();
    }

    private Rectangle computeSourceRect(Tile targetTile) {
        final Rectangle rectangle = targetTile.getRectangle();
        int x = rectangle.x - shiftX;
        int width = rectangle.width;
        if (x < 0) {
            x = rectangle.x;
            width -= shiftX;
        }
        int y = rectangle.y - shiftY;
        int height = rectangle.height;
        if (y < 0) {
            y = rectangle.y;
            height -= shiftY;
        }
        rectangle.setBounds(x, y, width, height);
        return rectangle;
    }

    private boolean isPositionOutOfBounds(int xPos, int yPos) {
        return xPos < 0 ||
               yPos < 0 ||
               xPos >= sourceProduct.getSceneRasterWidth() ||
               yPos >= sourceProduct.getSceneRasterHeight();
    }

    private void validateParameters() {
        if (fillValue == null) {
            throw new OperatorException("No fill value specified.");
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ShiftOp.class);
        }
    }

}
