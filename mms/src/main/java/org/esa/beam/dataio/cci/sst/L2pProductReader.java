/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.common.ImageVariableOpImage;
import org.esa.beam.common.NetcdfProductReaderTemplate;
import org.esa.beam.common.PixelLocator;
import org.esa.beam.common.PixelLocatorAdapter;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.PixelLocatorFactory;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Ralf Quast
 */
public class L2pProductReader extends NetcdfProductReaderTemplate {

    public L2pProductReader(L2pProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        final List<Variable> variables = getNetcdfFile().getVariables();
        for (final Variable variable : variables) {
            if (variable.getDimensionsString().contains("nj ni")) {
                final Band band = product.addBand(variable.getShortName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
                addValidPixelExpression(variable, band);
                addFlagCoding(variable, band, product);
            }
        }

        product.addMask("sst_uncertainty_suspicious",
                        "inrange(sea_surface_temperature, 271.15, 323.15) ? abs(sses_standard_deviation - sqrt(large_scale_correlated_uncertainty * large_scale_correlated_uncertainty + synoptically_correlated_uncertainty * synoptically_correlated_uncertainty + uncorrelated_uncertainty * uncorrelated_uncertainty)) > 0.01 : False",
                        "Mask used for visual product verification only",
                        Color.RED,
                        0.5);
        product.addMask("sst_depth_uncertainty_suspicious",
                        "inrange(sea_surface_temperature_depth, 271.15, 323.15) ? abs(sst_depth_total_uncertainty - sqrt(large_scale_correlated_uncertainty * large_scale_correlated_uncertainty + synoptically_correlated_uncertainty * synoptically_correlated_uncertainty + uncorrelated_uncertainty * uncorrelated_uncertainty + adjustment_uncertainty * adjustment_uncertainty)) > 0.01 : False",
                        "Mask used for visual product verification only",
                        Color.RED,
                        0.5);
    }

    @Override
    protected void addGeoCoding(Product product) {
        final Band latBand = product.getBand("lat");
        final Band lonBand = product.getBand("lon");
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonBand, latBand, 0);
        final GeoCoding geoCoding = new PixelLocatorAdapter(pixelLocator);

        product.setGeoCoding(geoCoding);
    }

    @Override
    protected void addMetadata(Product product) {
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), product.getMetadataRoot());
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File file = new File(getInput().toString());
        final int w = findDimension("ni").getLength();
        final int h = findDimension("nj").getLength();

        final Product product = new Product(file.getName(), L2pProductReaderPlugIn.FORMAT_NAME, w, h);
        product.setPreferredTileSize(w, w);

        return product;
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(band.getName());
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int w = band.getSceneRasterWidth();
        final int h = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new ImageVariableOpImage(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES) {
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
    protected void setTime(Product product) throws IOException {
        final int referenceTime = findVariable("time").read().getInt(0);
        final Variable timeOffset = findVariable("sst_dtime");
        final Array array = timeOffset.read();
        final short startTimeOffset = array.getShort(0);
        final short endTimeOffset = array.getShort((int) (array.getSize() - 1));

        final ProductData.UTC startTime = ProductData.UTC.create(
                TimeUtil.secondsSince1981ToDate(referenceTime + startTimeOffset), 0);
        final ProductData.UTC endTime = ProductData.UTC.create(
                TimeUtil.secondsSince1981ToDate(referenceTime + endTimeOffset), 0);

        product.setStartTime(startTime);
        product.setEndTime(endTime);
    }

}
