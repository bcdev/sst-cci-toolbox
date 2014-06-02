package org.esa.beam.dataio.cci.sst;

import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Ralf Quast
 */
public class NcAvhrrGacProductReader extends NetcdfProductReaderTemplate {

    private static final String VAR_NAME_QUALITY_FLAGS = "qual_flags";
    private static final String VAR_NAME_TIME = "time";
    private static final String VAR_NAME_DTIME = "dtime";

    private final ProfilePartReader flagCodingReader;
    private final ProfilePartReader indexCodingReader;

    private static final NumberInvalidator TIMING_INVALIDATOR = new NumberInvalidator() {
        @Override
        public final boolean isInvalid(Number value) {
            return (value.shortValue() & 4) == 4;
        }
    };

    protected NcAvhrrGacProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        flagCodingReader = new CfFlagCodingPart();
        indexCodingReader = new CfIndexCodingPart();
    }

    @Override
    protected void addBands(Product product) throws IOException {
        for (final Variable v : getNetcdfFile().getVariables()) {
            final String dimensionsString = v.getDimensionsString();
            if (dimensionsString.contains("nj ni")) {
                final String bandName = v.getShortName();
                final Band band = product.addBand(bandName, DataTypeUtils.getRasterDataType(v));
                CfBandPart.readCfBandAttributes(v, band);

                if (band.getValidPixelExpression() == null || band.getValidPixelExpression().isEmpty()) {
                    final Number validMin = getAttribute(v, "valid_min");
                    final Number validMax = getAttribute(v, "valid_max");

                    if (validMin != null && validMax != null) {
                        band.setValidPixelExpression(
                                String.format("%s >= %s && %s <= %s", bandName, validMin, bandName, validMax));
                    } else if (validMin != null) {
                        band.setValidPixelExpression(
                                String.format("%s >= %s", bandName, validMin));
                    } else if (validMax != null) {
                        band.setValidPixelExpression(
                                String.format("%s <= %s", bandName, validMax));
                    }
                }
            }
        }
    }

    @Override
    protected void addSampleCodings(Product product) throws IOException {
        flagCodingReader.decode(this, product);
        indexCodingReader.decode(this, product);
    }

    @Override
    protected void addGeoCoding(Product product) throws IOException {
        final Band latBand = product.getBand("lat");
        final Band lonBand = product.getBand("lon");
        final BasicPixelGeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "", 5);
        product.setGeoCoding(pixelGeoCoding);
    }

    @Override
    protected void addMetadata(Product product) {
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), product.getMetadataRoot());
    }

    @Override
    protected Product createPlainProduct() throws IOException {
        final File inputFile = new File(getNetcdfFile().getLocation());
        final String productName = inputFile.getName();
        final int w = findDimension("ni").getLength();
        final int h = findDimension("nj").getLength();

        return new Product(productName, NcAvhrrGacProductReaderPlugIn.FORMAT_NAME, w, h);
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
    protected void setTime(Product product) throws IOException {
        final Variable flags = findVariable(VAR_NAME_QUALITY_FLAGS);
        final int[] shape = columnShape(flags);
        final LineInvalidator lineInvalidator = new LineInvalidator(flags, TIMING_INVALIDATOR, shape);
        final int leadLineSkip = lineInvalidator.getLeadLineSkip();
        final int tailLineSkip = lineInvalidator.getTailLineSkip();

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

    private static int[] columnShape(Variable variable) {
        final int[] shape = variable.getShape();
        for (int i = 0; i < shape.length; i++) {
            if (i != variable.getRank() - 2) {
                shape[i] = 1;
            }
        }
        return shape;
    }

}
