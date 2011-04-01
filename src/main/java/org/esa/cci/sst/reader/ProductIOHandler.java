package org.esa.cci.sst.reader;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;

public class ProductIOHandler implements IOHandler {

    private final String sensorName;
    private final BoundaryCalculator bc;

    private DataFile dataFile;
    private Product product;


    public ProductIOHandler(String sensorName, BoundaryCalculator bc) {
        this.sensorName = sensorName;
        this.bc = bc;
    }

    @Override
    public final void init(DataFile dataFile) throws IOException {
        Product product = ProductIO.readProduct(dataFile.getPath());
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", dataFile.getPath()));
        }
        if (bc != null) {
            final ProductSubsetDef subsetDef = new ProductSubsetDef();
            try {
                subsetDef.setRegion(bc.getPixelBoundary(product));
            } catch (Exception e) {
                throw new IOException(e);
            }
            product = createProductSubset(product, subsetDef);
        }
        this.product = product;
        this.dataFile = dataFile;
    }

    @Override
    public final void close() {
        if (product != null) {
            product.dispose();
        }
    }

    @Override
    public final int getNumRecords() {
        return 1;
    }

    @Override
    public final Observation readObservation(int recordNo) throws IOException {
        final Observation observation;
        if (bc == null) {
            observation = new Observation();
        } else {
            final RelatedObservation relatedObservation = new RelatedObservation();
            try {
                relatedObservation.setLocation(createGeometry(bc.getGeoBoundary(product)));
            } catch (Exception e) {
                throw new IOException(e);
            }
            observation = relatedObservation;
        }

        observation.setDatafile(dataFile);
        observation.setRecordNo(0);
        observation.setSensor(sensorName);
        observation.setTime(getCenterTimeAsDate());

        return observation;
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final ArrayList<VariableDescriptor> variableDescriptorList = new ArrayList<VariableDescriptor>();
        for (RasterDataNode node : product.getTiePointGrids()) {
            final VariableDescriptor variableDescriptor = new VariableDescriptor();
            variableDescriptor.setName(String.format("%s.%s", sensorName, node.getName()));
            variableDescriptor.setDataSchema(dataFile.getDataSchema());
            final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
            variableDescriptor.setType(dataType.name());
            variableDescriptor.setDimensions("ni nj");
            variableDescriptor.setDimensionRoles("ni nj");
            if (node.isScalingApplied()) {
                variableDescriptor.setAddOffset(node.getScalingOffset());
                variableDescriptor.setScaleFactor(node.getScalingFactor());
            }
            if (node.isNoDataValueUsed()) {
                variableDescriptor.setFillValue(node.getNoDataValue());
            }
            variableDescriptor.setUnits(node.getUnit());
            variableDescriptorList.add(variableDescriptor);
        }
        for (RasterDataNode node : product.getBands()) {
            final VariableDescriptor variableDescriptor = new VariableDescriptor();
            variableDescriptor.setName(String.format("%s.%s", sensorName, node.getName()));
            variableDescriptor.setDataSchema(dataFile.getDataSchema());
            final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
            variableDescriptor.setType(dataType.name());
            variableDescriptor.setDimensions("ni nj");
            variableDescriptor.setDimensionRoles("ni nj");
            if (node.isScalingApplied()) {
                variableDescriptor.setAddOffset(node.getScalingOffset());
                variableDescriptor.setScaleFactor(node.getScalingFactor());
            }
            if (node.isNoDataValueUsed()) {
                variableDescriptor.setFillValue(node.getNoDataValue());
            }
            final String units = node.getUnit();
            if (units != null && !units.isEmpty()) {
                variableDescriptor.setUnits(units);
            }
            variableDescriptorList.add(variableDescriptor);
        }
        return variableDescriptorList.toArray(new VariableDescriptor[variableDescriptorList.size()]);
    }

    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVariableName,
                      String targetVariableName, int matchupIndex, final PGgeometry refPoint, final Date refTime) throws
                                                                                                                  IOException {
        final RasterDataNode node;
        if (product.containsBand(sourceVariableName)) {
            node = product.getBand(sourceVariableName);
        } else {
            node = product.getTiePointGrid(sourceVariableName);
        }

        final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
        final float lon = (float) refPoint.getGeometry().getFirstPoint().x;
        final float lat = (float) refPoint.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        if (!geoPos.isValid()) {
            throw new IOException("Invalid reference geo-location.");
        }
        final GeoCoding geoCoding = product.getGeoCoding();
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
        if (!pixelPos.isValid()) {
            throw new IOException(MessageFormat.format(
                    "Unable to find pixel at {0}, {1} in product ''{2}''.", geoPos.getLatString(),
                    geoPos.getLonString(), product.getName()));
        }
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));
        final int[] targetOrigin = new int[]{matchupIndex, 0, 0};
        final int[] targetShape = targetVariable.getShape();
        final Rectangle rectangle = createSubsceneRectangle(pixelPos, targetShape);
        final Array array = readSubsceneData(node, dataType, targetShape, rectangle);
        try {
            targetFile.write(NetcdfFile.escapeName(targetVariableName), targetOrigin, array);
        } catch (Exception e) {
            throw new IOException(MessageFormat.format(
                    "Unable to write subscene for product ''{0}''.", product.getName()), e);
        }
    }

    @Override
    public String getDataFilePath() {
        return dataFile.getPath();
    }

    private Date getCenterTimeAsDate() throws IOException {
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime == null) {
            throw new IOException("Unable to get start time for product '" + product.getName() + "'.");
        }
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime == null) {
            return startTime.getAsDate();
        }
        final ProductData.UTC centerTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        return centerTime.getAsDate();
    }

    private static PGgeometry createGeometry(Point[] geoBoundary) throws IOException {
        if (geoBoundary == null) {
            return null;
        }
        return new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(geoBoundary)}));
    }

    private static Rectangle createSubsceneRectangle(final PixelPos subsceneCenter, final int[] subsceneShape) {
        final int w = subsceneShape[2];
        final int h = subsceneShape[1];
        final int x = (int) Math.floor(subsceneCenter.getX()) - w / 2;
        final int y = (int) Math.floor(subsceneCenter.getY()) - h / 2;
        return new Rectangle(x, y, w, h);
    }

    private Product createProductSubset(Product product, ProductSubsetDef subsetDef) throws IOException {
        final Product subset = ProductSubsetBuilder.createProductSubset(product, true, subsetDef, product.getName(),
                                                                        product.getDescription());
        // work around Issue BEAM-1240
        final GeoCoding geoCoding = subset.getGeoCoding();
        if (geoCoding instanceof PixelGeoCoding) {
            final PixelGeoCoding pixelGeoCoding = (PixelGeoCoding) geoCoding;
            final Band latBand = pixelGeoCoding.getLatBand();
            final Band lonBand = pixelGeoCoding.getLonBand();
            final PixelSource latPixelSource = new RasterDataNodePixelSource(latBand);
            final PixelSource lonPixelSource = new RasterDataNodePixelSource(lonBand);
            subset.setGeoCoding(new SimplePixelGeoCoding(latPixelSource, lonPixelSource));
            // the original geo-codings of the subset and parent products can be disposed
            geoCoding.dispose();
            product.getGeoCoding().dispose();
            product.setGeoCoding(null);
        }
        return subset;
    }

    private static Array readSubsceneData(final RasterDataNode node, final DataType type, final int[] targetShape,
                                          final Rectangle rectangle) {
        final Array array = Array.factory(type, new int[]{1, targetShape[1], targetShape[2]});

        final MultiLevelImage sourceImage = node.getSourceImage();
        final int minX = sourceImage.getMinX();
        final int minY = sourceImage.getMinY();
        final int maxX = sourceImage.getMaxX();
        final int maxY = sourceImage.getMaxY();

        final int x = Math.max(rectangle.x, minX);
        final int y = Math.max(rectangle.y, minY);
        final int w = Math.min(rectangle.x + rectangle.width - 1, maxX) - x + 1;
        final int h = Math.min(rectangle.y + rectangle.height - 1, maxY) - y + 1;
        final Rectangle validRectangle = new Rectangle(x, y, w, h);
        final Raster raster = sourceImage.getData(validRectangle);

        for (int i = rectangle.y, k = 0; i < rectangle.y + rectangle.height; i++) {
            for (int j = rectangle.x; j < rectangle.x + rectangle.width; j++, k++) {
                final Number value;
                if (i < minY || i > maxY || j < minX || j > maxX) {
                    value = node.getNoDataValue();
                } else {
                    try {
                        value = getSample(raster, j, i);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                array.setObject(k, value);
            }
        }
        return array;
    }

    private static Number getSample(Raster raster, int x, int y) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                return raster.getSample(x, y, 0);
            case DataBuffer.TYPE_FLOAT:
                return raster.getSampleFloat(x, y, 0);
            case DataBuffer.TYPE_DOUBLE:
                return raster.getSampleDouble(x, y, 0);
            default:
                throw new IllegalArgumentException("Unsupported transfer type " + raster.getTransferType() + ".");
        }
    }

}
