/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.TimeUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class NcOsiProductReader extends NetcdfProductReaderTemplate {


    protected NcOsiProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            final String dimensionsString = variable.getDimensionsString();
            if (dimensionsString.contains("yc xc")) {
                final Band band = product.addBand(variable.getName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
            }
        }
    }

    @Override
    protected void addGeoCoding(Product product) {

        double northing = 0;
        double easting = 0;
        double pixelSizeX = 0;
        double pixelSizeY = 0;
        try {
            final Variable xcVariable = getNetcdfFile().findVariable("xc");
            final Variable ycVariable = getNetcdfFile().findVariable("yc");
            final Array xc = xcVariable.read();
            final Array yc = ycVariable.read();

            northing = xc.getDouble(0);
            pixelSizeX = (xc.getDouble(xcVariable.getShape(0) - 1) - northing) / xcVariable.getShape(0);

            easting = yc.getFloat(0);
            pixelSizeY = (yc.getDouble(xcVariable.getShape(0) - 1) - northing) / ycVariable.getShape(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        CrsGeoCoding geoCoding = null;
        try {
            String code = "EPSG:3411";
            geoCoding = new CrsGeoCoding(CRS.decode(code), width, height, easting, northing, pixelSizeX, pixelSizeY, 0.0, 0.0);
        } catch (FactoryException e) {
            e.printStackTrace();
        } catch (TransformException e) {
            e.printStackTrace();
        }
        product.setGeoCoding(geoCoding);
    }

    @Override
    protected void addMetadata(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), metadataRoot);
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        int width = getNetcdfFile().findDimension("xc").getLength();
        int height = getNetcdfFile().findDimension("yc").getLength();
        final String productName = new File(getInput().toString()).getName();
        return new Product(productName, NcOsiProductReaderPlugIn.FORMAT_NAME, width, height);
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(band.getName());
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new VariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES) {
            @Override
            protected final int getIndexX(int rank) {
                return rank - 1;
            }

            @Override
            protected final int getIndexY(int rank) {
                return rank - 2;
            }
        };
    }

    @Override
    protected void setTime(Product product) {
        final ProductData.UTC startTime = TimeUtils.getSceneRasterTime(getNetcdfFile(),
                                                                       Constants.START_DATE_ATT_NAME,
                                                                       Constants.START_TIME_ATT_NAME);
        final ProductData.UTC endTime = TimeUtils.getSceneRasterTime(getNetcdfFile(),
                                                                     Constants.STOP_DATE_ATT_NAME,
                                                                     Constants.STOP_TIME_ATT_NAME);
        product.setStartTime(startTime);
        product.setEndTime(endTime);
    }
}
