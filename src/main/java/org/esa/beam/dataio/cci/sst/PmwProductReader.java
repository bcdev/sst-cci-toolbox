package org.esa.beam.dataio.cci.sst;

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.TimeUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * Product reader for TMI and AMSR-E products.
 *
 * @author Ralf Quast
 */
public class PmwProductReader extends BasicNetcdfProductReader {

    protected PmwProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product createProduct() throws IOException {
        final File inputFile = new File(getNetcdfFile().getLocation());
        final String productName = inputFile.getName();
        final int h = getNetcdfFile().findDimension("ni").getLength();
        final int w = getNetcdfFile().findDimension("nj").getLength();

        return new Product(productName, getReaderPlugIn().getFormatNames()[0], w, h);
    }

    @Override
    protected final void addBands(Product product) {
        for (final Variable variable : getNetcdfFile().getVariables()) {
            if (variable.getDimensionsString().contains("nj ni")) {
                final Band band = product.addBand(variable.getName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
            }
        }
    }

    @Override
    protected final void addGeoCoding(Product product) {
        final Band lonBand = product.getBand("lon");
        final Band latBand = product.getBand("lat");
        if (latBand != null && lonBand != null) {
            final PixelGeoCoding geoCoding = new PixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5);
            product.setGeoCoding(geoCoding);
        }
    }

    @Override
    protected final void addMetadata(Product product) {
        MetadataUtils.readNetcdfMetadata(getNetcdfFile(), product.getMetadataRoot());
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(NetcdfFile.escapeName(band.getName()));
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new VariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES) {

            @Override
            protected int getIndexX(int rank) {
                return rank - 2;
            }

            @Override
            protected int getIndexY(int rank) {
                return rank - 1;
            }

            @Override
            protected final Object transformStorage(Array array) {
                return array.transpose(getIndexX(array.getRank()), getIndexY(array.getRank())).copyTo1DJavaArray();
            }
        };
    }

    @Override
    protected final void setTime(Product product) {
        final ProductData.UTC startTime = TimeUtils.getSceneRasterTime(getNetcdfFile(),
                                                                       Constants.START_DATE_ATT_NAME,
                                                                       Constants.START_TIME_ATT_NAME);
        final ProductData.UTC endTime = TimeUtils.getSceneRasterTime(getNetcdfFile(),
                                                                     Constants.STOP_DATE_ATT_NAME,
                                                                     Constants.STOP_TIME_ATT_NAME);
        product.setStartTime(startTime);
        product.setEndTime(endTime);
    }

}
