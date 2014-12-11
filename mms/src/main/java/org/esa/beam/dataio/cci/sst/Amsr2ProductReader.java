/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.TimeUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelLocator;
import org.esa.beam.framework.datamodel.PixelLocatorAdapter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.PixelLocatorFactory;
import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Quast
 */
public final class Amsr2ProductReader extends NetcdfProductReaderTemplate {

    private static final String TEMPLATE_VARIABLE_NAME;
    private static final String LAT_BAND_NAME = "latitude";
    private static final String LON_BAND_NAME = "longitude";
    private static final String PIXEL_DATA_QUALITY_BAND_NAME = "pixel_data_quality";

    static {
        TEMPLATE_VARIABLE_NAME = NetcdfFile.makeValidCDLName("Brightness_Temperature_(res06,10.7GHz,H)");
    }

    final Map<String, String> nameMapping = new HashMap<>();
    private int[] shape;

    public Amsr2ProductReader(Amsr2ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            final String variableName = variable.getShortName();
            if (Arrays.equals(shape, variable.getShape())) {
                if (variableName.contains("res06") || !variableName.startsWith("Brightness")) {
                    addBand(product, variable);
                }
            } else {
                if (variableName.contains("89A")) { // latitude and longitude
                    addBand(product, variable);
                }
                if (variableName.equals("Pixel_Data_Quality_89")) { // TODO - use Pixel_Data_Quality_6_to_36 instead?
                    addBand(product, variable);
                }
                if (variableName.contains("Time")) { // scan time
                    addBand(product, variable);
                }
            }
        }
        product.addBand("solar_zenith_angle", "sun_elevation + earth_incidence");
        product.addBand("solar_azimuth_angle", "(earth_azimuth - sun_azimuth + 180.0) % 360.0");
    }

    private void addBand(Product product, Variable variable) {
        final String sourceName = variable.getFullNameEscaped();
        final String targetName = toBandName(sourceName);
        nameMapping.put(targetName, sourceName);
        final Band band = product.addBand(targetName, DataTypeUtils.getRasterDataType(variable));
        final Attribute scaleFactor = variable.findAttribute("SCALE_FACTOR");
        if (scaleFactor != null) {
            band.setScalingFactor(scaleFactor.getNumericValue().doubleValue());
        }
        final Attribute unit = variable.findAttribute("UNIT");
        if (unit != null) {
            band.setUnit(unit.getStringValue());
        }
    }

    private String toBandName(String variableName) {
        if (variableName.startsWith("Latitude")) {
            return LAT_BAND_NAME;
        }
        if (variableName.startsWith("Longitude")) {
            return LON_BAND_NAME;
        }
        if (variableName.startsWith("Pixel")) {
            return PIXEL_DATA_QUALITY_BAND_NAME;
        }
        return variableName
                .replace("res06,", "")
                .replace("(", "")
                .replace(")", "")
                .replace("GHz", "")
                .replace(",", "_")
                .replace("\\.", "_")
                .toLowerCase();
    }

    @Override
    protected void addGeoCoding(Product product) throws IOException {
        final Band latBand = product.getBand(LAT_BAND_NAME);
        final Band lonBand = product.getBand(LON_BAND_NAME);
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonBand, latBand);
        final GeoCoding geoCoding = new PixelLocatorAdapter(pixelLocator);

        product.setGeoCoding(geoCoding);
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

        shape = findVariable(TEMPLATE_VARIABLE_NAME).getShape();
        final int w = shape[1];
        final int h = shape[0];

        final Product product = new Product(productName, NcOsiProductReaderPlugIn.FORMAT_NAME, w, h);
        product.setPreferredTileSize(w, w);

        return product;
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(nameMapping.get(band.getName()));
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int w = band.getSceneRasterWidth();
        final int h = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        if (variable.getRank() == 2) {
            if (Arrays.equals(shape, variable.getShape())) {
                return new VariableOpImage(variable, bufferType, w, h, tileSize);
            } else {
                return new OddVariableOpImage(variable, bufferType, w, h, tileSize);
            }
        } else {
            return new ScanLineVariableOpImage(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES) {
                @Override
                protected int getIndexY(int rank) {
                    return rank - 1;
                }
            };
        }
    }

    @Override
    protected void setTime(Product product) throws IOException {
        final String startTimeString = getNetcdfFile().findGlobalAttribute("ObservationStartDateTime").getStringValue();
        final String endTimeString = getNetcdfFile().findGlobalAttribute("ObservationEndDateTime").getStringValue();

        try {
            final Date startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString);
            final long startMillis = startTime.getTime() % 1000;
            product.setStartTime(ProductData.UTC.create(startTime, startMillis * 1000));

            final Date endTime = TimeUtil.parseCcsdsUtcFormat(endTimeString);
            final long endMillis = endTime.getTime() % 1000;
            product.setEndTime(ProductData.UTC.create(endTime, endMillis * 1000));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }


    @Override
    public void close() throws IOException {
        nameMapping.clear();
        super.close();
    }

    private static final class VariableOpImage extends ImageVariableOpImage {

        public VariableOpImage(Variable variable, int bufferType, int w, int h, Dimension tileSize) {
            super(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES);
        }

        @Override
        protected int getIndexX(int rank) {
            return rank - 1;
        }

        @Override
        protected int getIndexY(int rank) {
            return rank - 2;
        }
    }

    private static final class OddVariableOpImage extends ImageVariableOpImage {

        public OddVariableOpImage(Variable variable, int bufferType, int w, int h, Dimension tileSize) {
            super(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES);
        }

        @Override
        protected int getIndexX(int rank) {
            return rank - 1;
        }

        @Override
        protected int getIndexY(int rank) {
            return rank - 2;
        }

        @Override
        protected int getSourceOriginX() {
            return 0;
        }

        @Override
        protected int getSourceStrideX() {
            return 2;
        }

        @Override
        protected int getSourceOriginX(int x) {
            return super.getSourceOriginX(x) * 2;
        }

        @Override
        protected final int getSourceShapeX(int width) {
            return super.getSourceShapeX(width) * 2;
        }
    }
}
