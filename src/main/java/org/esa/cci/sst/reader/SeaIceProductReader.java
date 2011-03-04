package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.util.Debug;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * A BEAM reader for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class SeaIceProductReader extends AbstractProductReader {

    static final String NH_GRID = "OSISAF_NH";
    static final String SH_GRID = "OSISAF_SH";

    private static final String SEA_ICE_PARAMETER_BANDNAME = "sea_ice_concentration";
    private static final String QUALITY_FLAG_BANDNAME = "quality_flag";
    private static final String VARIABLE_NAME = "Data/" + NetcdfFile.escapeName("data[00]");
    private static final String DESCRIPTION_SEA_ICE = "A data product containing information about sea ice " +
                                                      "concentration: it indicates the areal fraction of a given grid " +
                                                      "point covered by ice.";
    private static final String DESCRIPTION_QUALITY_FLAG = "A data product containing a quality flag for sea ice " +
                                                           "concentration: it indicates the confidence level " +
                                                           "corresponding to the quality of the calculated sea ice " +
                                                           "parameter and information on the processing conditions.";

    private NetcdfFile ncFile;
    private int sceneRasterWidth;
    private int sceneRasterHeight;

    SeaIceProductReader(SeaIceProductReaderPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String pathname = getInput().toString();
        final File inputFile = new File(pathname);
        ncFile = NetcdfFile.open(inputFile.getPath());
        final Variable header = ncFile.findVariable("Header");
        final Structure headerStructure = (Structure) header;

        final String productName = headerStructure.findVariable("product").readScalarString();
        sceneRasterWidth = headerStructure.findVariable("iw").readScalarInt();
        sceneRasterHeight = headerStructure.findVariable("ih").readScalarInt();

        final int year = headerStructure.findVariable("year").readScalarInt();
        final int month = headerStructure.findVariable("month").readScalarInt();
        final int day = headerStructure.findVariable("day").readScalarInt();
        final int hour = headerStructure.findVariable("hour").readScalarInt();
        final int minute = headerStructure.findVariable("minute").readScalarInt();

        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], sceneRasterWidth,
                                            sceneRasterHeight);
        setTimes(product, year, month, day, hour, minute);
        final Band band;
        if (isSeaIceFile(pathname)) {
            band = product.addBand(SEA_ICE_PARAMETER_BANDNAME, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(-32767.0);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_SEA_ICE);
        } else {
            band = product.addBand(QUALITY_FLAG_BANDNAME, ProductData.TYPE_INT16);
            band.setNoDataValue(-32767);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_QUALITY_FLAG);
        }
        product.setFileLocation(inputFile);
        product.setProductReader(this);
        product.getMetadataRoot().addElement(getMetadata(headerStructure));
        product.setGeoCoding(createGeoCoding(headerStructure));
        // TODO - resulting image is wrong when tile size is different from image dimension
        product.setPreferredTileSize(sceneRasterWidth, sceneRasterHeight);
        band.setSourceImage(createSourceImage(band));
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
        ncFile.close();
        super.close();
    }

    MetadataElement getMetadata(Structure headerStructure) {
        final MetadataElement element = new MetadataElement("Header");
        for (Variable variable : headerStructure.getVariables()) {
            int type = DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), false, false);
            ProductData productData = null;
            try {
                switch (type) {
                    case ProductData.TYPE_ASCII: {
                        productData = ProductData.createInstance(variable.readScalarString());
                        break;
                    }
                    case ProductData.TYPE_INT8: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarByte()});
                        break;
                    }
                    case ProductData.TYPE_INT16: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                        break;
                    }
                    case ProductData.TYPE_INT32: {
                        productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                        break;
                    }
                    case ProductData.TYPE_FLOAT32: {
                        productData = ProductData.createInstance(new float[]{variable.readScalarFloat()});
                        break;
                    }
                    case ProductData.TYPE_FLOAT64: {
                        productData = ProductData.createInstance(new double[]{variable.readScalarDouble()});
                        break;
                    }
                    default: {
                        break;
                    }
                }
            } catch (IOException e) {
                Debug.trace(e.getMessage());
            }
            MetadataAttribute attribute = new MetadataAttribute(variable.getName(), productData, true);
            element.addAttribute(attribute);
        }
        return element;
    }

    GeoCoding createGeoCoding(Structure headerStructure) throws IOException {
        try {
            final String grid = headerStructure.findVariable("area").readScalarString();
            String code;
            if (NH_GRID.equals(grid)) {
                code = "EPSG:3411";
            } else if (SH_GRID.equals(grid)) {
                code = "EPSG:3412";
            } else {
                // code for computing math transform for higher latitude grid is to be found
                // in commit e9a32d1c6d18670c358f8e9434a7d2becb149449
                throw new IllegalStateException(
                        "Grid support for grids different from 'Northern Hemisphere Grid' and " +
                        "'Southern Hemisphere Grid' not yet implemented.");
            }
            final double easting = headerStructure.findVariable("Bx").readScalarFloat() * 1000.0;
            final double northing = headerStructure.findVariable("By").readScalarFloat() * 1000.0;
            final double pixelSizeX = headerStructure.findVariable("Ax").readScalarFloat() * 1000.0;
            final double pixelSizeY = headerStructure.findVariable("Ay").readScalarFloat() * 1000.0;
            return new CrsGeoCoding(CRS.decode(code),
                                    sceneRasterWidth,
                                    sceneRasterHeight,
                                    easting, northing,
                                    pixelSizeX,
                                    pixelSizeY, 0.0, 0.0);
        } catch (FactoryException e) {
            Debug.trace(e);
        } catch (TransformException e) {
            Debug.trace(e);
        }
        return null;
    }

    private NetcdfOpImage createSourceImage(Band band) {
        final Variable variable = ncFile.findVariable(VARIABLE_NAME);
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        return new NetcdfOpImage(variable, ncFile, dataBufferType, band.getSceneRasterWidth(),
                                 band.getSceneRasterHeight(),
        // TODO - resulting image is wrong when tile size is different from image dimension
                                 band.getProduct().getPreferredTileSize(),
                                 ResolutionLevel.MAXRES);
    }

    void setTimes(Product product, int year, int month, int day, int hour, int minute) {
        StringBuilder builder = new StringBuilder();
        builder.append(year);
        builder.append("-");
        if (month < 10) {
            builder.append("0");
        }
        builder.append(month);
        builder.append("-");
        if (day < 10) {
            builder.append("0");
        }
        builder.append(day);
        builder.append("-");
        if (hour < 10) {
            builder.append("0");
        }
        builder.append(hour);
        builder.append("-");
        if (minute < 10) {
            builder.append("0");
        }
        builder.append(minute);
        ProductData.UTC startTime = null;
        try {
            startTime = ProductData.UTC.parse(builder.toString(), "yyyy-MM-dd-HH-mm");
        } catch (ParseException e) {
            Debug.trace("No start time could be set due to the following exception:");
            Debug.trace(e);
        }
        product.setStartTime(startTime);
        product.setEndTime(startTime);
    }

    static boolean isSeaIceFile(String pathname) {
        return !pathname.contains("_qual_");
    }

    static Structure getHeaderStructure(List<Variable> variables) {
        for (Variable variable : variables) {
            if ("Header".equals(variable.getName()) && variable instanceof Structure) {
                return (Structure) variable;
            }
        }
        throw new IllegalStateException(
                "HDF-File does not contain a header variable; should not have been opened using '" +
                SeaIceProductReader.class.getSimpleName() + "'.");
    }

    private static class NetcdfOpImage extends SingleBandedOpImage {

        private final Variable variable;
        private final Object readLock;

        /**
         * Used to construct an image.
         *
         * @param variable       The netCDF variable
         * @param readLock       The the lock used for reading, usually the netcdf file that contains the variable
         * @param dataBufferType The data type.
         * @param sourceWidth    The width of the level 0 image.
         * @param sourceHeight   The height of the level 0 image.
         * @param tileSize       The tile size for this image.
         * @param level          The resolution level.
         */
        public NetcdfOpImage(Variable variable, Object readLock, int dataBufferType,
                             int sourceWidth, int sourceHeight,
                             Dimension tileSize, ResolutionLevel level) {
            super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
            this.variable = variable;
            this.readLock = readLock;
        }

        @Override
        protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
            Rectangle sourceRect;
            if (getLevel() != 0) {
                sourceRect = getSourceRect(destRect);
            } else {
                sourceRect = destRect;
            }

            final int rank = variable.getRank();
            final int[] origin = new int[rank];
            final int[] shape = new int[rank];
            final int[] stride = new int[rank];
            for (int i = 0; i < rank; i++) {
                shape[i] = 1;
                origin[i] = 0;
                stride[i] = 1;
            }
            final int xIndex = rank - 2;
            final int yIndex = rank - 1;

            shape[yIndex] = sourceRect.height;
            shape[xIndex] = sourceRect.width;

            origin[yIndex] = sourceRect.y;
            origin[xIndex] = sourceRect.x;

            double scale = getScale();
            stride[yIndex] = (int) scale;
            stride[xIndex] = (int) scale;

            Array array;
            synchronized (readLock) {
                try {
                    final Section section = new Section(origin, shape, stride);
                    array = variable.read(section);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } catch (InvalidRangeException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            tile.setDataElements(destRect.x, destRect.y,
                                 destRect.width, destRect.height,
                                 array.getStorage());
        }

        private Rectangle getSourceRect(Rectangle rect) {
            int sourceX = getSourceX(rect.x);
            int sourceY = getSourceY(rect.y);
            int sourceWidth = getSourceWidth(rect.width);
            int sourceHeight = getSourceHeight(rect.height);
            return new Rectangle(sourceX, sourceY, sourceWidth, sourceHeight);
        }
    }
}
