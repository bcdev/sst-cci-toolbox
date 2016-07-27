package org.esa.beam.dataio.amsre;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.common.Default2DOpImage;
import org.esa.beam.common.ImageVariableOpImage;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class AmsreProductReader extends AbstractProductReader {

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

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
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

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

    private String getProductName(Variable coreMetaVariable) throws IOException {
        if (coreMetaVariable == null) {
            return "unknown";
        }
        final String coreMeta = coreMetaVariable.readScalarString();
        final String productName = extractProductName(coreMeta);
        return FileUtils.getFilenameWithoutExtension(productName);
    }

    private Product createProduct() throws IOException {
        final Group rootGroup = netcdfFile.findGroup(null);
        final Variable coreMetaVariable = netcdfFile.findVariable(rootGroup, "CoreMetadata.0");
        final String productName = getProductName(coreMetaVariable);
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

    private void addBandsFromGroup(Product product, Group geoLocationGroup) {
        final List<Variable> variables = geoLocationGroup.getVariables();
        for (final Variable variable : variables) {
            final int dataType = DataTypeUtils.getRasterDataType(variable);
            final Band band = product.addBand(variable.getShortName(), dataType);

            final int bufferType = ImageManager.getDataBufferType(band.getDataType());
            final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();
            final int width = band.getSceneRasterWidth();
            final int height = band.getSceneRasterHeight();
            final Default2DOpImage defaultOpImage = new Default2DOpImage(variable, bufferType, width, height, tileSize);
            band.setSourceImage(defaultOpImage);
        }
    }

    // Package access for testing only tb 2016-07-26
    static String assembleUTCString(String dateString, String timeString) {
        String startDateString = dateString + "T" + timeString;
        final int lastDotIndex = startDateString.lastIndexOf('.');
        startDateString = startDateString.substring(0, lastDotIndex);
        return startDateString;
    }
}
