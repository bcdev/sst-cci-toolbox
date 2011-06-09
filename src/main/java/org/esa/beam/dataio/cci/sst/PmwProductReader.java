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

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * Product reader for TMI and AMSR-E products.
 *
 * @author Ralf Quast
 */
public class PmwProductReader extends NetcdfProductReaderTemplate {

    private static final Invalidator AMS_LAT_INVALIDATOR = new Invalidator() {
        @Override
        public final boolean isInvalid(double value) {
            return value > 90.0 || value < -90.0 || Double.isNaN(value);
        }
    };
    private static final Invalidator TMI_LAT_INVALIDATOR = new Invalidator() {
        @Override
        public final boolean isInvalid(double value) {
            return value > 90.0 || value < -90.0 || value == 0;
        }
    };

    private int leadLineSkip;
    private int tailLineSkip;

    protected PmwProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File inputFile = new File(getNetcdfFile().getLocation());
        final String productName = inputFile.getName();

        if (inputFile.getName().matches(PmwProductReaderPlugIn.AMS_FILE_NAME_PATTERN)) {
            invalidateLines(AMS_LAT_INVALIDATOR);
        } else {
            invalidateLines(TMI_LAT_INVALIDATOR);
        }
        final int h = findDimension("ni").getLength() - leadLineSkip - tailLineSkip;
        final int w = findDimension("nj").getLength();

        return new Product(productName, getReaderPlugIn().getFormatNames()[0], w, h);
    }

    @Override
    protected final void addBands(Product product) {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            if (variable.getDimensionsString().contains("nj ni")) {
                final Band band = product.addBand(variable.getName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
            }
        }
    }

    @Override
    protected final void addGeoCoding(Product product) {
        final Band lonBand = product.getBand("lon");
        final Band latBand = product.getBand("lat");
        if (latBand != null && lonBand != null) {
            final GeoCoding geoCoding = new PixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5);
            product.setGeoCoding(geoCoding);
        }
    }

    @Override
    protected final void addMetadata(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), metadataRoot);

        final MetadataElement generated = new MetadataElement("reader_generated");
        generated.addAttribute(new MetadataAttribute("lead_line_skip",
                                                     ProductData.createInstance(new int[]{leadLineSkip}), true));
        generated.addAttribute(new MetadataAttribute("tail_line_skip",
                                                     ProductData.createInstance(new int[]{tailLineSkip}), true));
        metadataRoot.addElement(generated);
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(NetcdfFile.escapeName(band.getName()));
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new VariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES) {

            @Override
            protected int getIndexX(int rank) {
                return rank - 2;
            }

            @Override
            protected int getIndexY(int rank) {
                return rank - 1;
            }

            @Override
            protected final int getSourceOriginY() {
                return leadLineSkip;
            }

            @Override
            protected final Object transformStorage(Array array) {
                return array.transpose(getIndexX(array.getRank()), getIndexY(array.getRank())).copyTo1DJavaArray();
            }
        };
    }

    @Override
    protected final void setTime(Product product) {
        try {
            final double referenceTime = findVariable("time").read().getDouble(0);
            final Variable variable = findVariable("sst_dtime");
            final int[] shape = columnShape(variable);
            final Array array = variable.read(new int[shape.length], shape);
            final short startDTime = array.getShort(leadLineSkip);
            final short endDTime = array.getShort(leadLineSkip + product.getSceneRasterHeight() - 1);

            final ProductData.UTC startTime = ProductData.UTC.create(
                    TimeUtil.secondsSince1981ToDate(referenceTime + 10 * startDTime), 0);
            final ProductData.UTC endTime = ProductData.UTC.create(
                    TimeUtil.secondsSince1981ToDate(referenceTime + 10 * endDTime), 0);

            product.setStartTime(startTime);
            product.setEndTime(endTime);
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void invalidateLines(Invalidator invalidator) throws IOException {
        final Variable variable = findVariable("lat");
        final int[] shape = columnShape(variable);
        try {
            final Array array = variable.read(new int[shape.length], shape);
            for (int i = 0, lineCount = shape[variable.getRank() - 1]; i < lineCount; i++) {
                if (invalidator.isInvalid(array.getDouble(i))) {
                    leadLineSkip++;
                } else {
                    break;
                }
            }
            for (int i = variable.getShape(variable.getRank() - 1); i-- > 0; ) {
                if (invalidator.isInvalid(array.getDouble(i))) {
                    tailLineSkip++;
                } else {
                    break;
                }
            }
        } catch (InvalidRangeException ignored) {
            // cannot happen
        }
    }

    private static int[] columnShape(Variable variable) {
        final int[] shape = variable.getShape();
        for (int i = 0; i < shape.length - 1; i++) {
            shape[i] = 1;
        }
        return shape;
    }

    private interface Invalidator {

        boolean isInvalid(double value);
    }

}
