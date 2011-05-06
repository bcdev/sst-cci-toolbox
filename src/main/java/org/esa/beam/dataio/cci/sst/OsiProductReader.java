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

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.Debug;
import org.geotools.referencing.CRS;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * A BEAM reader for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class OsiProductReader extends NetcdfProductReaderTemplate {

    static final String NH_GRID = "OSISAF_NH";
    static final String SH_GRID = "OSISAF_SH";

    private static final String SEA_ICE_PARAMETER_BANDNAME = "sea_ice_concentration";
    private static final String QUALITY_FLAG_BANDNAME = "quality_flag";
    private static final String VARIABLE_NAME = "Data/" + NetcdfFile.escapeName("data[00]");
    private static final String DESCRIPTION_SEA_ICE = "A data product containing information about sea ice " +
                                                      "concentration: it indicates the areal fraction of a given grid " +
                                                      "point covered by ice.";
    private static final String DESCRIPTION_QUALITY_FLAG = "A data product containing a quality flag for sea ice " +
                                                           "concentration: it indicates the confidence level " +
                                                           "corresponding to the quality of the calculated sea ice " +
                                                           "parameter and information on the processing conditions.";

    OsiProductReader(OsiProductReaderPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final Structure headerStructure = getHeaderStructure();
        final String productName = headerStructure.findVariable("product").readScalarString();
        final int w = headerStructure.findVariable("iw").readScalarInt();
        final int h = headerStructure.findVariable("ih").readScalarInt();

        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], w, h);
        // IMPORTANT - resulting image is wrong when tile size is different from image dimension
        product.setPreferredTileSize(w, h);

        return product;
    }

    @Override
    protected final void addBands(Product product) {
        if (isSeaIceFile(product.getFileLocation())) {
            final Band band = product.addBand(SEA_ICE_PARAMETER_BANDNAME, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(-32767.0);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_SEA_ICE);
        } else {
            final Band band = product.addBand(QUALITY_FLAG_BANDNAME, ProductData.TYPE_INT16);
            band.setNoDataValue(-32767);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_QUALITY_FLAG);
        }
    }

    @Override
    protected final void addGeoCoding(Product product) {
        final GeoCoding geoCoding;
        try {
            geoCoding = createGeoCoding(getHeaderStructure());
            product.setGeoCoding(geoCoding);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected final void addMetadata(Product product) {
        product.getMetadataRoot().addElement(getMetadata(getHeaderStructure()));
    }

    @Override
    protected final RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(VARIABLE_NAME);
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new VariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES) {
            @Override
            protected final int getIndexX(int rank) {
                return rank - 2;
            }

            @Override
            protected final int getIndexY(int rank) {
                return rank - 1;
            }
        };
    }

    @Override
    protected final void setTime(Product product) {
        final Structure headerStructure = getHeaderStructure();
        try {
            final int year = headerStructure.findVariable("year").readScalarInt();
            final int month = headerStructure.findVariable("month").readScalarInt();
            final int day = headerStructure.findVariable("day").readScalarInt();
            final int hour = headerStructure.findVariable("hour").readScalarInt();
            final int minute = headerStructure.findVariable("minute").readScalarInt();
            setTime(product, year, month, day, hour, minute);
        } catch (IOException ignored) {
        }
    }

    MetadataElement getMetadata(Structure headerStructure) {
        final MetadataElement element = new MetadataElement("Header");
        for (Variable variable : headerStructure.getVariables()) {
            int type = DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), false, false);
            ProductData productData = null;
            try {
                switch (type) {
                    case ProductData.TYPE_ASCII: {
                        productData = ProductData.createInstance(variable.readScalarString());
                        break;
                    }
                    case ProductData.TYPE_INT8: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarByte()});
                        break;
                    }
                    case ProductData.TYPE_INT16: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                        break;
                    }
                    case ProductData.TYPE_INT32: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                        break;
                    }
                    case ProductData.TYPE_FLOAT32: {
                        productData = ProductData.createInstance(new float[]{variable.readScalarFloat()});
                        break;
                    }
                    case ProductData.TYPE_FLOAT64: {
                        productData = ProductData.createInstance(new double[]{variable.readScalarDouble()});
                        break;
                    }
                    default: {
                        break;
                    }
                }
            } catch (IOException e) {
                Debug.trace(e.getMessage());
            }
            MetadataAttribute attribute = new MetadataAttribute(variable.getName(), productData, true);
            element.addAttribute(attribute);
        }
        return element;
    }

    GeoCoding createGeoCoding(Structure headerStructure) throws Exception {
        final int w = headerStructure.findVariable("iw").readScalarInt();
        final int h = headerStructure.findVariable("ih").readScalarInt();
        final String grid = headerStructure.findVariable("area").readScalarString();
        String code;
        if (NH_GRID.equals(grid)) {
            code = "EPSG:3411";
        } else if (SH_GRID.equals(grid)) {
            code = "EPSG:3412";
        } else {
            // code for computing math transform for higher latitude grid is to be found
            // in commit e9a32d1c6d18670c358f8e9434a7d2becb149449
            throw new IllegalStateException(
                    "Grid support for grids different from 'Northern Hemisphere Grid' and " +
                    "'Southern Hemisphere Grid' not yet implemented.");
        }
        final double easting = headerStructure.findVariable("Bx").readScalarFloat() * 1000.0;
        final double northing = headerStructure.findVariable("By").readScalarFloat() * 1000.0;
        final double pixelSizeX = headerStructure.findVariable("Ax").readScalarFloat() * 1000.0;
        final double pixelSizeY = headerStructure.findVariable("Ay").readScalarFloat() * 1000.0;

        return new CrsGeoCoding(CRS.decode(code), w, h, easting, northing, pixelSizeX, pixelSizeY, 0.0, 0.0);
    }


    private Structure getHeaderStructure() {
        return (Structure) getNetcdfFile().findVariable("Header");
    }

    void setTime(Product product, int year, int month, int day, int hour, int minute) {
        final Calendar calendar = ProductData.UTC.createCalendar();

        calendar.set(year, month - 1, day, hour - 12, minute);
        final ProductData.UTC startTime = ProductData.UTC.create(calendar.getTime(), 0);

        calendar.set(year, month - 1, day, hour + 12, minute);
        final ProductData.UTC endTime = ProductData.UTC.create(calendar.getTime(), 0);

        product.setStartTime(startTime);
        product.setEndTime(endTime);
    }

    static boolean isSeaIceFile(File file) {
        return !file.getName().contains("_qual_");
    }

}
