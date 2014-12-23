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

import org.esa.beam.common.ImageVariableOpImage;
import org.esa.beam.common.NetcdfProductReaderTemplate;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Product reader for AAI files in netcdf-format.
 *
 * @author Thomas Storm
 */
public class NcAaiProductReader extends NetcdfProductReaderTemplate {

    public NcAaiProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        final List<Variable> variables = getNetcdfFile().getVariables();
        for (final Variable variable : variables) {
            if (variable.getDimensionsString().contains("ny nx")) {
                final Band band = product.addBand(variable.getShortName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
            }
        }
        final Band lonBand = product.addBand("lon", "LON", ProductData.TYPE_FLOAT32);
        lonBand.setUnit("Degrees east");
        final Band latBand = product.addBand("lat", "LAT", ProductData.TYPE_FLOAT32);
        latBand.setUnit("Degrees north");
    }

    @Override
    protected void addGeoCoding(Product product) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final double scaleX = 360.0 / width;
        final double scaleY = 180.0 / height;
        final AffineTransform transform = new AffineTransform();
        transform.translate(-180.0, -90.0);
        transform.scale(scaleX, scaleY);

        try {
            final Rectangle imageBounds = new Rectangle(width, height);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, transform);
            product.setGeoCoding(geoCoding);
        } catch (FactoryException | TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void addMetadata(Product product) {
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), product.getMetadataRoot());
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File file = new File(getInput().toString());
        final int width = findDimension("nx").getLength();
        final int height = findDimension("ny").getLength();

        final Product product = new Product(file.getName(), "AerosolAai", width, height);
        product.setPreferredTileSize(width, height);

        return product;
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(band.getName());
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new ImageVariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES) {
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
        final String name = product.getName();
        if (name.matches("aai_[0-9]{8}.*\\.nc")) {
            final String timeString = name.substring(4, 12);
            try {
                final ProductData.UTC startTime = ProductData.UTC.parse(timeString, "yyyyMMdd");
                product.setStartTime(startTime);
                final ProductData.UTC endTime = new ProductData.UTC(startTime.getDaysFraction() + 1, 0, 0);
                product.setEndTime(endTime);
            } catch (ParseException ignored) {
            }
        }
    }

}
