package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.NetcdfFile;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * Basic reader for netCDF product files.
 *
 * @author Ralf Quast
 */
abstract class BasicNetcdfProductReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;

    protected BasicNetcdfProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    @Override
    protected final Product readProductNodesImpl() throws IOException {
        final File inputFile;
        if (getInput() instanceof File) {
            inputFile = (File) getInput();
        } else {
            inputFile = new File(getInput().toString());
        }
        netcdfFile = NetcdfFile.open(inputFile.getPath());
        return createProduct(netcdfFile);
    }

    @Override
    protected final synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                             int sourceWidth, int sourceHeight,
                                                             int sourceStepX, int sourceStepY,
                                                             Band destBand,
                                                             int targetOffsetX, int targetOffsetY,
                                                             int targetWidth, int targetHeight,
                                                             ProductData targetBuffer,
                                                             ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(targetOffsetX, targetOffsetY, targetWidth, targetHeight));
        data.getDataElements(targetOffsetX, targetOffsetY, targetWidth, targetHeight, targetBuffer.getElems());
    }

    @Override
    public final void close() throws IOException {
        netcdfFile.close();
        super.close();
    }

    protected abstract Product createProduct(NetcdfFile netcdfFile) throws IOException;

    protected abstract RenderedImage createSourceImage(Band band);
}
