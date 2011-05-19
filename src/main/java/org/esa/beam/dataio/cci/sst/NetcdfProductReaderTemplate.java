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

package org.esa.beam.dataio.cci.sst;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.NetcdfFile;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * A template for netCDF product file readers.
 *
 * @author Ralf Quast
 */
abstract class NetcdfProductReaderTemplate extends AbstractProductReader {

    private NetcdfFile netcdfFile;

    protected NetcdfProductReaderTemplate(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    @Override
    protected final Product readProductNodesImpl() throws IOException {
        final File inputFile = new File(getInput().toString());
        netcdfFile = NetcdfFile.open(inputFile.getPath());
        final Product product = createPlainProduct();
        product.setProductReader(this);
        product.setFileLocation(inputFile);
        addMetadata(product);
        addBands(product);
        addGeoCoding(product);
        for (final Band band : product.getBands()) {
            band.setSourceImage(createSourceImage(band));
        }
        setTime(product);

        return product;
    }

    @Override
    protected final synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                             int sourceWidth, int sourceHeight,
                                                             int sourceStepX, int sourceStepY,
                                                             Band destBand,
                                                             int targetOffsetX, int targetOffsetY,
                                                             int targetWidth, int targetHeight,
                                                             ProductData targetBuffer,
                                                             ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(targetOffsetX, targetOffsetY, targetWidth, targetHeight));
        data.getDataElements(targetOffsetX, targetOffsetY, targetWidth, targetHeight, targetBuffer.getElems());
    }

    @Override
    public final void close() throws IOException {
        if (netcdfFile != null) {
            try {
                netcdfFile.close();
            } catch (IOException ignored) {
            }
            netcdfFile = null;
        }
        super.close();
    }

    protected abstract void addBands(Product product) throws IOException;

    protected abstract void addGeoCoding(Product product);

    protected abstract void addMetadata(Product product);

    protected abstract Product createPlainProduct() throws IOException;

    protected abstract RenderedImage createSourceImage(Band band);

    protected abstract void setTime(Product product);
}
