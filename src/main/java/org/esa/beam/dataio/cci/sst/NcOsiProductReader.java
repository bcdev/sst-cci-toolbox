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
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.TimeUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
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

    private static final double KM = 1000.0;

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
    protected void addGeoCoding(Product product) throws IOException {
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        try {
            final Array x = getVariable("xc").read();
            final Array y = getVariable("yc").read();
            final Array lat = getVariable("lat").read(new int[]{w / 2, h / 2}, new int[]{1, 1});

            final double easting = x.getDouble(0) * KM;
            final double northing = y.getDouble(0) * KM;
            final double sizeX = (x.getDouble(w - 1) * KM - easting) / (w - 1);
            final double sizeY = (northing - y.getDouble(h - 1) * KM) / (h - 1);

            final String code;
            if (lat.getDouble(0) > 0.0) {
                code = "EPSG:3411";
            } else {
                code = "EPSG:3412";
            }
            final CoordinateReferenceSystem crs = CRS.decode(code);
            final GeoCoding gc = new CrsGeoCoding(crs, w, h, easting, northing, sizeX, sizeY, 0.5, 0.5);
            product.setGeoCoding(gc);
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void addMetadata(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), metadataRoot);
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File inputFile = new File(getNetcdfFile().getLocation());
        final String productName = inputFile.getName();
        final int w = getNetcdfFile().findDimension("xc").getLength();
        final int h = getNetcdfFile().findDimension("yc").getLength();

        return new Product(productName, NcOsiProductReaderPlugIn.FORMAT_NAME, w, h);
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
