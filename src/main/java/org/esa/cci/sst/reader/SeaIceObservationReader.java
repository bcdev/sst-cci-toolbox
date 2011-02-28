package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.util.Debug;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
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
public class SeaIceObservationReader extends AbstractProductReader {

    private static final String SEA_ICE_PARAMETER_BANDNAME = "sea_ice_concentration";
    private static final String QUALITY_FLAG_BANDNAME = "quality_flag";
    private static final String VARIABLE_NAME = "Data/" + NetcdfFile.escapeName("data[00]");

    private NetcdfFile ncFile;

    public SeaIceObservationReader(SeaIceObservationReaderPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String pathname = getInput().toString();
        final File inputFile = new File(pathname);
        ncFile = NetcdfFile.open(inputFile.getPath());
        final List<Variable> variables = ncFile.getVariables();
        final Structure headerStructure = getHeaderStructure(variables);

        final String productName = getVariable("Header.product", headerStructure).readScalarString();
        final int sceneRasterWidth = getVariable("Header.iw", headerStructure).readScalarInt();
        final int sceneRasterHeight = getVariable("Header.ih", headerStructure).readScalarInt();

        int year = getVariable("Header.year", headerStructure).readScalarInt();
        int month = getVariable("Header.month", headerStructure).readScalarInt();
        int day = getVariable("Header.day", headerStructure).readScalarInt();
        int hour = getVariable("Header.hour", headerStructure).readScalarInt();
        int minute = getVariable("Header.minute", headerStructure).readScalarInt();

        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], sceneRasterWidth,
                                            sceneRasterHeight);
        setStartTime(product, year, month, day, hour, minute);
        final Band band;
        if (isQualityFlagFile(pathname)) {
            band = product.addBand(QUALITY_FLAG_BANDNAME, ProductData.TYPE_INT16);
            band.setNoDataValue(-32767);
            band.setNoDataValueUsed(true);
        } else {
            band = product.addBand(SEA_ICE_PARAMETER_BANDNAME, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(-32767.0);
            band.setNoDataValueUsed(true);
        }
        try {
            final double easting = getVariable("Header.Bx", headerStructure).readScalarFloat() * 1000.0;
            final double northing = getVariable("Header.By", headerStructure).readScalarFloat() * 1000.0;
            final double pixelSizeX = getVariable("Header.Ax", headerStructure).readScalarFloat() * 1000.0;
            final double pixelSizeY = getVariable("Header.Ay", headerStructure).readScalarFloat() * 1000.0;
            final GeoCoding geoCoding = new CrsGeoCoding(CRS.decode("EPSG:3411"),
                                                         sceneRasterWidth,
                                                         sceneRasterHeight,
                                                         easting, northing,
                                                         pixelSizeX,
                                                         pixelSizeY, 0.0, 0.0);
            product.setGeoCoding(geoCoding);
        } catch (FactoryException e) {
            // ignore
        } catch (TransformException e) {
            // ignore
        }
        band.setSourceImage(createSourceImage(band));
        return product;
    }

    private NetcdfOpImage createSourceImage(Band band) {
        final Variable variable = ncFile.findVariable(VARIABLE_NAME);
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        // TODO - resulting image is wrong when more than a single tile is used
        return new NetcdfOpImage(variable, ncFile, dataBufferType, band.getSceneRasterWidth(),
                                 band.getSceneRasterHeight(),
                                 new Dimension(band.getRasterWidth(), band.getSceneRasterHeight()),
                                 ResolutionLevel.MAXRES);
    }

    @Override
    public void close() throws IOException {
        ncFile.close();
        super.close();
    }

    void setStartTime(Product product, int year, int month, int day, int hour, int minute) {
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
    }

    Variable getVariable(String varName, Structure headerStructure) {
        for (final Variable variable : headerStructure.getVariables()) {
            if (varName.equals(variable.getName())) {
                return variable;
            }
        }
        throw new IllegalArgumentException("No variable with name '" + varName + "'.");
    }

    static boolean isQualityFlagFile(String pathname) {
        return pathname.contains("_qual_");
    }

    static Structure getHeaderStructure(List<Variable> variables) {
        for (Variable variable : variables) {
            if ("Header".equals(variable.getName()) && variable instanceof Structure) {
                return (Structure) variable;
            }
        }
        throw new IllegalStateException(
                "HDF-File does not contain a header variable; should not have been opened using '" +
                SeaIceObservationReader.class.getSimpleName() + "'.");
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

    private static MathTransform createStereographicMathTransform(GeodeticDatum datum) throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters;

        parameters = transformFactory.getDefaultParameters("EPSG:9809");

        parameters.parameter("semi_major").setValue(datum.getEllipsoid().getSemiMajorAxis());
        parameters.parameter("semi_minor").setValue(datum.getEllipsoid().getSemiMinorAxis());
        parameters.parameter("central_meridian").setValue(-45.0);
        parameters.parameter("latitude_of_origin").setValue(90.0);
        parameters.parameter("scale_factor").setValue(1.0);
        parameters.parameter("false_easting").setValue(0.0);
        parameters.parameter("false_northing").setValue(0.0);

        return transformFactory.createParameterizedTransform(parameters);
    }

}
