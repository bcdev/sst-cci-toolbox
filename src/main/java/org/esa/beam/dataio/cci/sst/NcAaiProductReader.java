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

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Product reader for AAI files in netcdf-format.
 *
 * @author Thomas Storm
 */
public class NcAaiProductReader extends NetcdfProductReaderTemplate {

    private static final String VARIABLE_NAME = "aerosol_absorbing_index";

    public NcAaiProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        final Variable variable = getNetcdfFile().findVariable(VARIABLE_NAME);
        final int dataType = DataTypeUtils.getRasterDataType(variable);
        final String variableName = variable.getName();
        final Band band = product.addBand(variableName, dataType);
        final Attribute fillValueAttribute = variable.findAttribute("_FillValue");
        if(fillValueAttribute != null) {
            band.setNoDataValue((Float) fillValueAttribute.getNumericValue());
            band.setNoDataValueUsed(true);
        }
    }

    @Override
    protected void addGeoCoding(Product product) {
        final Dimension xDimension = getNetcdfFile().findDimension("nx");
        final Dimension yDimension = getNetcdfFile().findDimension("ny");
        final int width = xDimension.getLength();
        final int height = yDimension.getLength();
        final double scaleX = 360.0 / width;
        final double scaleY = 180.0 / height;
        final AffineTransform transform = new AffineTransform();
        transform.translate(-180.0, 90.0);
        transform.scale(scaleX, -scaleY);

        try {
            final Rectangle imageBounds = new Rectangle(width, height);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, transform);
            product.setGeoCoding(geoCoding);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("dimension", e);
        } catch (TransformException e) {
            throw new IllegalArgumentException("dimension", e);
        }
    }

    @Override
    protected void addMetadata(Product product) {
        final MetadataElement element = new MetadataElement("Global Attributes");
        final List<Attribute> globalAttributes = getNetcdfFile().getGlobalAttributes();
        for (Attribute ncAttribute : globalAttributes) {
            ProductData productData = DataTypeUtils.createProductData(ncAttribute);
            final MetadataAttribute attribute = new MetadataAttribute(ncAttribute.getName(), productData, true);
            element.addAttribute(attribute);
        }
        product.getMetadataRoot().addElement(element);
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File file = new File(getInput().toString());
        final int width = getNetcdfFile().findDimension("nx").getLength();
        final int height = getNetcdfFile().findDimension("ny").getLength();
        final Product product = new Product(file.getName(), "AerosolAai", width, height, this);
        product.setFileLocation(file);
        return product;
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
        final String timeString = getTimeString();
        if (timeString == null) {
            return;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM hh:mm:ss yyyy");
        try {
            final Date date = sdf.parse(timeString);
            product.setStartTime(ProductData.UTC.create(date, 0));
        } catch (ParseException ignored) {
            // ok; set no time, then.
        }
    }

    String getTimeString() {
        final List<Attribute> globalAttributes = getNetcdfFile().getGlobalAttributes();
        for (Attribute globalAttribute : globalAttributes) {
            if (globalAttribute.getName().equalsIgnoreCase("creation_date")) {
                return globalAttribute.getStringValue();
            }
        }
        return null;
    }
}
