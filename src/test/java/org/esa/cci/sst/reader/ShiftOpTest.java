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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.util.jai.SingleBandedSampleModel;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ShiftOpTest {

    private static final int FILL_VALUE = Integer.MIN_VALUE;

    @Before
    public void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    @Test
    public void testDontShift() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", 10);
        parameters.put("shiftY", 12);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "xyxyxyxyyxxyx");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final DataBuffer data = shiftedProduct.getBand("test").getSourceImage().getData().getDataBuffer();
        for (int i = 0; i < data.getSize(); i++) {
            assertEquals(i, data.getElem(i));
        }
    }

    @Test
    public void testDontShift2() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", 0);
        parameters.put("shiftY", 0);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "test");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final DataBuffer data = shiftedProduct.getBand("test").getSourceImage().getData().getDataBuffer();
        for (int i = 0; i < data.getSize(); i++) {
            assertEquals(i, data.getElem(i));
        }
    }

    @Test
    public void testShiftRight() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", 1);
        parameters.put("shiftY", 0);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "test");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final MultiLevelImage sourceImage = shiftedProduct.getBand("test").getSourceImage();
        final Raster data = sourceImage.getData();
        assertEquals(FILL_VALUE, data.getSample(0, 0, 0), 0.0);
        assertEquals(0, data.getSample(1, 0, 0));
        assertEquals(1, data.getSample(2, 0, 0));
        assertEquals(2, data.getSample(3, 0, 0));
        assertEquals(998, data.getSample(999, 0, 0));
        assertEquals(FILL_VALUE, data.getSample(0, 1, 0), 0.0);
        assertEquals(1000, data.getSample(1, 1, 0));
        assertEquals(1001, data.getSample(2, 1, 0));
        assertEquals(1002, data.getSample(3, 1, 0));
        assertEquals(1008, data.getSample(9, 1, 0));
        assertEquals(FILL_VALUE, data.getSample(0, 2, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(0, 3, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(0, 4, 0), 0.0);
    }

    @Test
    public void testShiftLeft() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", -1);
        parameters.put("shiftY", 0);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "test");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final MultiLevelImage sourceImage = shiftedProduct.getBand("test").getSourceImage();
        final Raster data = sourceImage.getData();
        assertEquals(1, data.getSample(0, 0, 0));
        assertEquals(2, data.getSample(1, 0, 0));
        assertEquals(3, data.getSample(2, 0, 0));
        assertEquals(FILL_VALUE, data.getSample(999, 0, 0), 0.0);
        assertEquals(1001, data.getSample(0, 1, 0));
        assertEquals(1002, data.getSample(1, 1, 0));
        assertEquals(1003, data.getSample(2, 1, 0));
        assertEquals(FILL_VALUE, data.getSample(999, 1, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(999, 998, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(999, 999, 0), 0.0);
    }

    @Test
    public void testShiftDown() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", 0);
        parameters.put("shiftY", 1);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "test");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final MultiLevelImage sourceImage = shiftedProduct.getBand("test").getSourceImage();
        final Raster data = sourceImage.getData();
        assertEquals(FILL_VALUE, data.getSample(0, 0, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(1, 0, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(2, 0, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(3, 0, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(9, 0, 0), 0.0);
        assertEquals(0, data.getSample(0, 1, 0));
        assertEquals(1, data.getSample(1, 1, 0));
        assertEquals(2, data.getSample(2, 1, 0));
        assertEquals(999, data.getSample(999, 1, 0));
        assertEquals(998000, data.getSample(0, 999, 0));
        assertEquals(998001, data.getSample(1, 999, 0));
        assertEquals(998002, data.getSample(2, 999, 0));
        assertEquals(998999, data.getSample(999, 999, 0));
    }

    @Test
    public void testShiftUp() throws Exception {
        final Product product = createDummyProduct();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("shiftX", 0);
        parameters.put("shiftY", -1);
        parameters.put("fillValue", FILL_VALUE);
        parameters.put("bandNamesPattern", "test");
        final Product shiftedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ShiftOp.class), parameters, product);
        final MultiLevelImage sourceImage = shiftedProduct.getBand("test").getSourceImage();
        final Raster data = sourceImage.getData();
        assertEquals(1000, data.getSample(0, 0, 0));
        assertEquals(1001, data.getSample(1, 0, 0));
        assertEquals(1002, data.getSample(2, 0, 0));
        assertEquals(1999, data.getSample(999, 0, 0));
        assertEquals(2000, data.getSample(0, 1, 0));
        assertEquals(2001, data.getSample(1, 1, 0));
        assertEquals(2002, data.getSample(2, 1, 0));
        assertEquals(2999, data.getSample(999, 1, 0));
        assertEquals(FILL_VALUE, data.getSample(1, 999, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(2, 999, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(3, 999, 0), 0.0);
        assertEquals(FILL_VALUE, data.getSample(999, 999, 0), 0.0);
    }

    private Product createDummyProduct() {
        final Product product = new Product("dummy", "type", 1000, 1000);
        final Band band = product.addBand("test", ProductData.TYPE_INT32);
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(2);
        imageLayout.setTileHeight(2);
        band.setSourceImage(new MySourcelessOpImage(imageLayout, new SingleBandedSampleModel(DataBuffer.TYPE_INT, 1000, 1000)));
        band.setNoDataValue(FILL_VALUE);
        return product;
    }

    private static class MySourcelessOpImage extends SourcelessOpImage {

        MySourcelessOpImage(ImageLayout imageLayout, SampleModel sampleModel) {
            super(imageLayout, null, sampleModel, 0, 0, 1000, 1000);
        }

        @Override
        public boolean computesUniqueTiles() {
            return super.computesUniqueTiles();
        }

        @Override
        public Raster computeTile(int tileX, int tileY) {
            final Point location = new Point(tileXToX(tileX), tileYToY(tileY));
            final WritableRaster raster = WritableRaster.createWritableRaster(getSampleModel(), location);
            for (int x = 0; x < raster.getWidth(); x++) {
                for (int y = 0; y < raster.getHeight(); y++) {
                    int sample = raster.getWidth() * tileX + x + raster.getHeight() * tileY * getWidth() + y * getWidth();
                    final int xPos = raster.getWidth() * tileX + x;
                    final int yPos = raster.getHeight() * tileY + y;
                    raster.setSample(xPos, yPos, 0, sample);
                }
            }
            return raster;
        }
    }
}
