package org.esa.beam.dataio.cci.sst;

import org.esa.beam.common.ImageVariableOpImage;
import org.esa.beam.common.NetcdfProductReaderTemplate;
import org.esa.beam.common.ScanLineVariableOpImage;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.common.PixelLocator;
import org.esa.beam.common.PixelLocatorAdapter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.PixelLocatorFactory;
import org.esa.beam.util.StringUtils;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Ralf Quast
 */
public class NcAvhrrGacProductReader extends NetcdfProductReaderTemplate {

    public static final String ELEMENT_NAME_GLOBAL_ATTRIBUTES = "Global_Attributes";

    private static final String VAR_NAME_QUALITY_FLAGS = "qual_flags";
    private static final String VAR_NAME_TIME = "time";
    private static final String VAR_NAME_DTIME = "dtime";

    private final ProfilePartReader indexCodingReader;

    private int leadLineSkip;
    private int tailLineSkip;

    private static final NumberInvalidator TIMING_INVALIDATOR = new NumberInvalidator() {
        @Override
        public final boolean isInvalid(Number value) {
            return (value.shortValue() & 4) == 4;
        }
    };

    protected NcAvhrrGacProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        indexCodingReader = new CfIndexCodingPart();
    }

    @Override
    protected void addBands(Product product) throws IOException {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            final String dimensionsString = variable.getDimensionsString();
            if (dimensionsString.contains("nj ni") || dimensionsString.contains("nj")) {
                final String bandName = variable.getShortName();
                final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
                final Band band = product.addBand(bandName, rasterDataType);
                CfBandPart.readCfBandAttributes(variable, band);

                addValidPixelExpression(variable, band);

                addFlagCoding(variable, band, product);
            }
        }
    }

    @Override
    protected void addSampleCodings(Product product) throws IOException {
        indexCodingReader.decode(this, product);
    }

    @Override
    protected void addExtraBands(Product product) {
        if (!product.containsBand("ch3a")) {
            final Band ch3a = product.addBand("ch3a", "-32768", ProductData.TYPE_INT16);
            /*
                ch3a:_FillValue = -32768s ;
                ch3a:units = "reflectance" ;
                ch3a:add_offset = 0.f ;
                ch3a:scale_factor = 0.0001f ;
             */
            ch3a.setNoDataValue(-32768.0);
            ch3a.setNoDataValueUsed(true);
            ch3a.setUnit("reflectance");
            ch3a.setScalingOffset(0.0);
            ch3a.setScalingFactor(0.0001);
        }
        if (!product.containsBand("ch3b")) {
            final Band ch3b = product.addBand("ch3b", "-32768", ProductData.TYPE_INT16);
            /*
                ch3b:_FillValue = -32768s ;
                ch3b:units = "kelvin" ;
                ch3b:add_offset = 273.15f ;
                ch3b:scale_factor = 0.01f ;
             */
            ch3b.setNoDataValue(-32768.0);
            ch3b.setNoDataValueUsed(true);
            ch3b.setUnit("kelvin");
            ch3b.setScalingOffset(273.15);
            ch3b.setScalingFactor(0.01);
        }
        if (!product.containsBand("ch5")) {
            final Band ch5 = product.addBand("ch5", "-32768", ProductData.TYPE_INT16);
            /*
                ch3b:_FillValue = -32768s ;
                ch3b:units = "kelvin" ;
                ch3b:add_offset = 273.15f ;
                ch3b:scale_factor = 0.01f ;
             */
            ch5.setNoDataValue(-32768.0);
            ch5.setNoDataValueUsed(true);
            ch5.setUnit("kelvin");
            ch5.setScalingOffset(273.15);
            ch5.setScalingFactor(0.01);
        }
    }

    @Override
    protected void addGeoCoding(Product product) throws IOException {
        final Band latBand = product.getBand("lat");
        final Band lonBand = product.getBand("lon");
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonBand, latBand, false);
        final GeoCoding geoCoding = new PixelLocatorAdapter(pixelLocator);

        product.setGeoCoding(geoCoding);
    }

    @Override
    protected void addMetadata(Product product) {
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
    protected Product createPlainProduct() throws IOException {
        final Variable flags = findVariable(VAR_NAME_QUALITY_FLAGS);
        final int[] shape = columnShape(flags);
        final LineInvalidator lineInvalidator = new LineInvalidator(flags, TIMING_INVALIDATOR, shape);
        leadLineSkip = lineInvalidator.getLeadLineSkip();
        tailLineSkip = lineInvalidator.getTailLineSkip();

        final File inputFile = new File(getNetcdfFile().getLocation());
        final String productName = inputFile.getName();
        final int w = findDimension("ni").getLength();
        final int h = findDimension("nj").getLength() - leadLineSkip - tailLineSkip;

        final Product product = new Product(productName, NcAvhrrGacProductReaderPlugIn.FORMAT_NAME, w, h);
        product.setPreferredTileSize(w, w);

        return product;
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(band.getName());
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();

        if (variable.getRank() > 2) {
            return new ImageVariableOpImageImpl(variable, bufferType, sourceWidth, sourceHeight, tileSize);
        } else {
            return new ScanLineVariableOpImageImpl(variable, bufferType, sourceWidth, sourceHeight, tileSize);
        }
    }

    @Override
    protected void setTime(Product product) throws IOException {
        final Variable flags = findVariable(VAR_NAME_QUALITY_FLAGS);
        final int[] shape = columnShape(flags);

        try {
            final int referenceTime = findVariable(VAR_NAME_TIME).read().getInt(0);
            final Variable timeOffset = findVariable(VAR_NAME_DTIME);
            final Array array = timeOffset.read(new int[shape.length], shape);
            final short startTimeOffset = array.getShort(leadLineSkip);
            final short endTimeOffset = array.getShort(product.getSceneRasterHeight() - 1 - tailLineSkip);

            final ProductData.UTC startTime = ProductData.UTC.create(
                    TimeUtil.secondsSince1981ToDate(referenceTime + startTimeOffset), 0);
            final ProductData.UTC endTime = ProductData.UTC.create(
                    TimeUtil.secondsSince1981ToDate(referenceTime + endTimeOffset), 0);

            product.setStartTime(startTime);
            product.setEndTime(endTime);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    static void addValidPixelExpression(Variable variable, Band band) {
        if (band.getValidPixelExpression() == null || band.getValidPixelExpression().isEmpty()) {
            final Number validMin = getAttribute(variable, "valid_min");
            final Number validMax = getAttribute(variable, "valid_max");

            final String bandName = band.getName();
            if (validMin != null && validMax != null) {
                band.setValidPixelExpression(
                        String.format("%s.raw >= %s && %s.raw <= %s", bandName, validMin, bandName, validMax));
            } else if (validMin != null) {
                band.setValidPixelExpression(String.format("%s.raw >= %s", bandName, validMin));
            } else if (validMax != null) {
                band.setValidPixelExpression(String.format("%s.raw <= %s", bandName, validMax));
            }
        }
    }

    static void addFlagCoding(Variable variable, Band band, Product product) {
        final Attribute masksAttribute = variable.findAttribute("flag_masks");
        final Attribute meaningsAttribute = variable.findAttribute("flag_meanings");
        if (masksAttribute == null || meaningsAttribute == null) {
            return;
        }

        final int masksLength = masksAttribute.getLength();
        final String meaningsString = meaningsAttribute.getStringValue();
        final String[] flagMeanings = StringUtils.split(meaningsString, new char[]{' '}, true);
        if (masksLength != flagMeanings.length) {
            return;
        }

        final String variableFullName = variable.getFullName();
        final FlagCoding flagCoding = new FlagCoding(variableFullName);
        for (int i = 0; i < flagMeanings.length; i++) {
            final Number maskNumerical = masksAttribute.getNumericValue(i);
            flagCoding.addFlag(flagMeanings[i], DataType.unsignedShortToInt(maskNumerical.shortValue()), null);
        }

        band.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);
    }

    private static int[] columnShape(Variable variable) {
        final int[] shape = variable.getShape();
        for (int i = 0; i < shape.length; i++) {
            if (i != variable.getRank() - 2) {
                shape[i] = 1;
            }
        }
        return shape;
    }

    private class ImageVariableOpImageImpl extends ImageVariableOpImage {

        public ImageVariableOpImageImpl(Variable variable, int bufferType, int sourceWidth, int sourceHeight,
                                        Dimension tileSize) {
            super(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES);
        }

        @Override
        protected final int getIndexX(int rank) {
            return rank - 1;
        }

        @Override
        protected final int getIndexY(int rank) {
            return rank - 2;
        }

        @Override
        protected int getSourceOriginY() {
            return leadLineSkip;
        }
    }

    private class ScanLineVariableOpImageImpl extends ScanLineVariableOpImage {

        public ScanLineVariableOpImageImpl(Variable variable, int bufferType, int sourceWidth, int sourceHeight,
                                           Dimension tileSize) {
            super(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES);
        }

        @Override
        protected final int getIndexY(int rank) {
            return rank - 1;
        }

        @Override
        protected int getSourceOriginY() {
            return leadLineSkip;
        }
    }
}
