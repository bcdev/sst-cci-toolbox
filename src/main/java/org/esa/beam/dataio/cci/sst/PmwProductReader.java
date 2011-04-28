package org.esa.beam.dataio.cci.sst;

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.Array;
import ucar.ma2.Index;
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
    protected Product createProduct(NetcdfFile netcdfFile) throws IOException {
        final File inputFile = new File(netcdfFile.getLocation());
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
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(NetcdfFile.escapeName(band.getName()));
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final Dimension tileSize = band.getProduct().getPreferredTileSize();

        return new VariableOpImage(variable, dataBufferType, sourceWidth, sourceHeight, tileSize) {
            @Override
            protected Object getStorage(Array array) {
                final Index index = array.getIndexPrivate();
                final int h = index.getShape(array.getRank() - 1);
                final int w = index.getShape(array.getRank() - 2);
                for (int j = 0, j0 = 0; j < w; j++, j0 += h) {
                    for (int i = 0, ij = j, ji = j0; i < h && ij < ji; i++, ij += w, ji++) {
                        final Object value1 = array.getObject(ij);
                        final Object value2 = array.getObject(ji);
                        array.setObject(ij, value2);
                        array.setObject(ji, value1);
                    }
                }
                return array.getStorage();
            }
        };
    }

}
