package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class PmwProductReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;

    protected PmwProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile;
        if (getInput() instanceof File) {
            inputFile = (File) getInput();
        } else {
            inputFile = new File(getInput().toString());
        }
        netcdfFile = NetcdfFile.open(inputFile.getPath());
        final String productName = inputFile.getName();
        final int h = netcdfFile.findDimension("ni").getLength();
        final int w = netcdfFile.findDimension("nj").getLength();
        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], w, h);
        product.setProductReader(this);
        product.setFileLocation(inputFile);
        // todo - set times
        for (final Variable variable : netcdfFile.getVariables()) {
            if (variable.getDimensionsString().contains("nj ni")) {
                final Band band = product.addBand(variable.getName(), DataTypeUtils.getRasterDataType(variable));
                CfBandPart.readCfBandAttributes(variable, band);
                band.setSourceImage(createSourceImage(band));
            }
        }
        // todo - geo-coding
        return product;
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                                       int sourceHeight,
                                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                       int destOffsetY, int destWidth, int destHeight,
                                                       ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));

        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    @Override
    public void close() throws IOException {
        netcdfFile.close();
        super.close();
    }

    private RenderedImage createSourceImage(Band band) {
        final Variable variable = netcdfFile.findVariable(NetcdfFile.escapeName(band.getName()));
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        return new TransposedVariableOpImage(variable, dataBufferType,
                                             band.getSceneRasterWidth(),
                                             band.getSceneRasterHeight(),
                                             band.getProduct().getPreferredTileSize()
        );
    }

    private static class TransposedVariableOpImage extends VariableOpImage {

        private TransposedVariableOpImage(Variable variable, int dataBufferType, int sourceWidth, int sourceHeight,
                                          Dimension tileSize) {
            super(variable, dataBufferType, sourceWidth, sourceHeight, tileSize);
        }

        @Override
        protected Object transform(Array array) {
            final Array transposedArray = Array.factory(array.getElementType(), array.getShape());
            final int h = array.getIndexPrivate().getShape(array.getRank() - 1);
            final int w = array.getIndexPrivate().getShape(array.getRank() - 2);
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    transposedArray.setObject(j + i * w, array.getObject(i + j * h));
                }
            }
            return transposedArray.getStorage();
        }
    }

}
