package org.esa.beam.dataio.amsre;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AmsreProductReader extends AbstractProductReader {

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
}
