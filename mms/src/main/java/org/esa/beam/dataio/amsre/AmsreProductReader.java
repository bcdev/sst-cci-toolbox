package org.esa.beam.dataio.amsre;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.common.Default2DOpImage;
import org.esa.beam.common.DefaultScanLineVariableOpImage;
import org.esa.beam.common.PixelLocator;
import org.esa.beam.common.PixelLocatorAdapter;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.PixelLocatorFactory;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.*;
import ucar.nc2.Dimension;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class AmsreProductReader extends AbstractProductReader {

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final int[] CHANNEL_QUALITY_FLAG_WAVELENGTHS = new int[] {6, 10, 18, 23, 36};

    private NetcdfFile netcdfFile;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected AmsreProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = new File(getInput().toString());
        netcdfFile = NetcdfFile.open(inputFile.getPath());

        final Product product = createProduct();

        addSensingTimes(product);
        addBands(product);
        addGeoCoding(product);

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    static String extractProductName(String coreMeta) {
        final int localGranuleIdStart = coreMeta.indexOf("LOCALGRANULEID");
        final int valueIndex = coreMeta.indexOf("VALUE", localGranuleIdStart);
        final int copyStart = coreMeta.indexOf("\"", valueIndex) + 1;
        final int copyEnd = coreMeta.indexOf("\"", copyStart);
        return coreMeta.substring(copyStart, copyEnd);
    }

    private String getProductName() throws IOException {
        final Group rootGroup = netcdfFile.findGroup(null);
        final Variable coreMetaVariable = netcdfFile.findVariable(rootGroup, "CoreMetadata.0");
        if (coreMetaVariable == null) {
            throw new IOException("Global attribute 'CoreMetadata.0' not found");
        }
        final String coreMeta = coreMetaVariable.readScalarString();
        final String productName = extractProductName(coreMeta);
        return FileUtils.getFilenameWithoutExtension(productName);
    }

    private Product createProduct() throws IOException {
        final String productName = getProductName();
        final String productType = getReaderPlugIn().getFormatNames()[0];

        final Variable latitude = netcdfFile.findVariable("/Low_Res_Swath/Geolocation_Fields/Latitude");
        final List<Dimension> dimensions = latitude.getDimensions();
        final int width = dimensions.get(1).getLength();
        final int height = dimensions.get(0).getLength();

        return new Product(productName, productType, width, height);
    }

    private void addSensingTimes(Product product) throws IOException {
        final Attribute rangeBeginningDateAttribute = netcdfFile.findGlobalAttribute("RangeBeginningDate");
        final Attribute rangeBeginningTimeAttribute = netcdfFile.findGlobalAttribute("RangeBeginningTime");
        final Attribute rangeEndingDateAttribute = netcdfFile.findGlobalAttribute("RangeEndingDate");
        final Attribute rangeEndingTimeAttribute = netcdfFile.findGlobalAttribute("RangeEndingTime");

        final String startDateString = assembleUTCString(rangeBeginningDateAttribute.getStringValue(), rangeBeginningTimeAttribute.getStringValue());
        try {
            final ProductData.UTC sensingStart = ProductData.UTC.parse(startDateString, DATE_PATTERN);
            product.setStartTime(sensingStart);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        final String endDateString = assembleUTCString(rangeEndingDateAttribute.getStringValue(), rangeEndingTimeAttribute.getStringValue());
        try {
            final ProductData.UTC sensingStop = ProductData.UTC.parse(endDateString, DATE_PATTERN);
            product.setEndTime(sensingStop);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void addBands(Product product) {
        final Group loResGroup = netcdfFile.findGroup("Low_Res_Swath");
        final Group geoLocationGroup = loResGroup.findGroup("Geolocation_Fields");
        addBandsFromGroup(product, geoLocationGroup);

        final Group dataGroup = loResGroup.findGroup("Data_Fields");
        addBandsFromGroup(product, dataGroup);
    }

    private void addBandsFromGroup(Product product, Group group) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final String variableName = variable.getShortName();
            if (variableName.startsWith("Channel_Quality_Flag")) {

            } else {
                final Band band = addBand(product, variable);
                addSourceImage(variable, band);
            }
        }
    }

    private void addSourceImage(Variable variable, Band band) {
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();
        final int width = band.getSceneRasterWidth();
        final int height = band.getSceneRasterHeight();

        final int rank = variable.getRank();
        if (rank == 1) {
            final DefaultScanLineVariableOpImage opImage = new DefaultScanLineVariableOpImage(variable, bufferType, width, height, tileSize);
            band.setSourceImage(opImage);
        } else {
            final Default2DOpImage defaultOpImage = new Default2DOpImage(variable, bufferType, width, height, tileSize);
            band.setSourceImage(defaultOpImage);
        }
    }

    private Band addBand(Product product, Variable variable) {
        final int dataType = DataTypeUtils.getRasterDataType(variable);
        final String shortName = variable.getShortName();
        final String bandName = removeDots(shortName);
        return product.addBand(bandName, dataType);
    }

    private void addGeoCoding(Product product) throws IOException {
        final Band latBand = product.getBand("Latitude");
        final Band lonBand = product.getBand("Longitude");
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonBand, latBand, 1);
        final GeoCoding geoCoding = new PixelLocatorAdapter(pixelLocator);

        product.setGeoCoding(geoCoding);
    }

    // Package access for testing only tb 2016-07-26
    static String assembleUTCString(String dateString, String timeString) {
        String startDateString = dateString + "T" + timeString;
        final int lastDotIndex = startDateString.lastIndexOf('.');
        startDateString = startDateString.substring(0, lastDotIndex);
        return startDateString;
    }

    static String removeDots(String variableName) {
        return variableName.replaceAll("\\.", "_");
    }
}
