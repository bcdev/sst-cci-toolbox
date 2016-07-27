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

package org.esa.beam.dataio.amsr2;

import org.esa.beam.common.Default2DOpImage;
import org.esa.beam.common.ImageVariableOpImage;
import org.esa.beam.common.NetcdfProductReaderTemplate;
import org.esa.beam.common.PixelLocator;
import org.esa.beam.common.PixelLocatorAdapter;
import org.esa.beam.common.ScanLineVariableOpImage;
import org.esa.beam.dataio.cci.sst.NcOsiProductReaderPlugIn;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.PixelLocatorFactory;
import org.esa.cci.sst.util.ByteConversion;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.BorderDescriptor;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
    private static final String PIXEL_DATA_QUALITY_6_TO_36_BAND_NAME = "pixel_data_quality_6";
    private static final String PIXEL_DATA_QUALITY_89_BAND_NAME = "pixel_data_quality_89";
    private static final String SCAN_DATA_QUALITY_BAND_NAME = "scan_data_quality";
    private static final String LAND_OCEAN_FLAG_BAND_NAME = "land_ocean_flag_6";

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
                    addBand(product, variable, DataTypeUtils.getRasterDataType(variable));
                }
            } else {
                if (variableName.contains("89A")) { // latitude and longitude
                    addBand(product, variable, DataTypeUtils.getRasterDataType(variable));
                }
                if (variableName.equals("Pixel_Data_Quality_6_to_36")) {
                    addBand(product, variable, ProductData.TYPE_UINT16);
                }
                if (variableName.equals("Pixel_Data_Quality_89")) {
                    addBand(product, variable, DataTypeUtils.getRasterDataType(variable));
                }
                if (variableName.equals("Scan_Data_Quality")) {
                    addBand(product, variable, ProductData.TYPE_UINT32);
                }
                if (variableName.equals("Land_Ocean_Flag_6_to_36")) {
                    addBand(product, variable, DataTypeUtils.getRasterDataType(variable));
                }
                if (variableName.contains("Time")) { // scan time
                    addBand(product, variable, DataTypeUtils.getRasterDataType(variable));
                }
            }
        }
        product.addBand("solar_zenith_angle", "sun_elevation + earth_incidence");
        product.addBand("solar_azimuth_angle", "(earth_azimuth - sun_azimuth + 180.0) % 360.0");
    }

    private void addBand(Product product, Variable variable, int dataType) {
        final String sourceName = variable.getFullNameEscaped();
        final String targetName = toBandName(sourceName);
        final Band band = product.addBand(targetName, dataType);
        final Attribute scaleFactor = variable.findAttribute("SCALE_FACTOR");
        if (scaleFactor != null) {
            final double value = scaleFactor.getNumericValue().doubleValue();
            if (value != 1.0) {
                band.setScalingFactor(value);
            }
        }
        final Attribute unit = variable.findAttribute("UNIT");
        if (unit != null) {
            band.setUnit(unit.getStringValue());
        }
        nameMapping.put(targetName, sourceName);
    }

    private String toBandName(String variableName) {
        if (variableName.startsWith("Latitude")) {
            return LAT_BAND_NAME;
        }
        if (variableName.startsWith("Longitude")) {
            return LON_BAND_NAME;
        }
        if (variableName.startsWith("Pixel") && variableName.endsWith("36")) {
            return PIXEL_DATA_QUALITY_6_TO_36_BAND_NAME;
        }
        if (variableName.startsWith("Pixel") && variableName.endsWith("89")) {
            return PIXEL_DATA_QUALITY_89_BAND_NAME;
        }
        if (variableName.startsWith("Land")) {
            return LAND_OCEAN_FLAG_BAND_NAME;
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
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonBand, latBand, 1);
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
                return new Default2DOpImage(variable, bufferType, w, h, tileSize);
            } else {
                if (PIXEL_DATA_QUALITY_6_TO_36_BAND_NAME.equals(band.getName())) {
                    return new UShortFromUByteOpImage(variable, w, h, tileSize);
                } else if (SCAN_DATA_QUALITY_BAND_NAME.equals(band.getName())) {
                    final RenderedImage image = new UIntFromUByteOpImage(variable, 128, h,
                                                                         new Dimension(128, tileSize.height));
                    return extend(w, h, tileSize, image);
                } else {
                    return new OddColumnsOpImage(variable, bufferType, w, h, tileSize);
                }
            }
        } else if (variable.getRank() == 1) {
            return new ScanLineVariableOpImage(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES) {
                @Override
                protected int getIndexY(int rank) {
                    return rank - 1;
                }
            };
        } else {
            return new Default2DOpImage(variable, bufferType, w, h, tileSize);
        }
    }

    private static RenderedImage extend(int w, int h, Dimension tileSize, RenderedImage image) {
        final BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        final ImageLayout layout = new ImageLayout(0, 0, w, h, 0, 0, tileSize.width, tileSize.height, null, null);
        final RenderingHints rh = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        return BorderDescriptor.create(image, null, w - image.getWidth(), null, null, borderExtender, rh);
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

    private static final class OddColumnsOpImage extends ImageVariableOpImage {

        public OddColumnsOpImage(Variable variable, int bufferType, int w, int h, Dimension tileSize) {
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

    private static final class UShortFromUByteOpImage extends ImageVariableOpImage {

        public UShortFromUByteOpImage(Variable variable, int w, int h, Dimension tileSize) {
            super(variable, DataBuffer.TYPE_USHORT, w, h, tileSize, ResolutionLevel.MAXRES);
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
        protected int getSourceOriginX(int x) {
            return super.getSourceOriginX(x) * 2;
        }

        @Override
        protected final int getSourceShapeX(int width) {
            return super.getSourceShapeX(width) * 2;
        }

        @Override
        protected Object transformStorage(Array array) {
            final byte[] bytes = (byte[]) array.getStorage();
            final short[] shorts = new short[bytes.length >> 1];

            for (int i = 0; i < shorts.length; ++i) {
                final byte hi = bytes[i * 2];
                final byte lo = bytes[i * 2 + 1];
                shorts[i] = (short) ((lo & 0xFF) | (hi & 0xFF) << 8);
            }
            return shorts;
        }
    }

    private static class UIntFromUByteOpImage extends ImageVariableOpImage {

        public UIntFromUByteOpImage(Variable variable, int w, int h, Dimension tileSize) {
            super(variable, DataBuffer.TYPE_INT, w, h, tileSize, ResolutionLevel.MAXRES);
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
        protected int getSourceOriginX(int x) {
            return super.getSourceOriginX(x) * 4;
        }

        @Override
        protected final int getSourceShapeX(int width) {
            return super.getSourceShapeX(width) * 4;
        }

        @Override
        protected Object transformStorage(Array array) {
            return ByteConversion.bytesToInts((byte[]) array.getStorage());
        }
    }

}
