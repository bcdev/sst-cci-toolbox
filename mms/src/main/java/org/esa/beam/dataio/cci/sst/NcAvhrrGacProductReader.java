package org.esa.beam.dataio.cci.sst;

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Ralf Quast
 */
public class NcAvhrrGacProductReader extends NetcdfProductReaderTemplate {

    protected NcAvhrrGacProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void addBands(Product product) throws IOException {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            final String dimensionsString = variable.getDimensionsString();
            if (dimensionsString.contains("nj ni")) {
                final Band band = product.addBand(variable.getShortName(), DataTypeUtils.getRasterDataType(
                        variable));
                CfBandPart.readCfBandAttributes(variable, band);
            }
        }
    }

    @Override
    protected void addGeoCoding(Product product) throws IOException {

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
        try {
            final int secondsSince1981 = findVariable("time").read().getInt(0);

            final ProductData.UTC startTime = ProductData.UTC.create(
                    TimeUtil.secondsSince1981ToDate(secondsSince1981), 0);

            product.setStartTime(startTime);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
