package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.Constants;
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
import ucar.ma2.InvalidRangeException;
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
    private final GeoBoundaryCalculator gbc;

    private DataFile dataFile;
    private Product product;


    public ProductIOHandler(String sensorName, GeoBoundaryCalculator gbc) {
        this.sensorName = sensorName;
        this.gbc = gbc;
    }

    @Override
    public final void init(DataFile dataFile) throws IOException {
        final Product product = ProductIO.readProduct(dataFile.getPath());
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", dataFile.getPath()));
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
        if (gbc == null) {
            observation = new Observation();
        } else {
            final RelatedObservation relatedObservation = new RelatedObservation();
            relatedObservation.setLocation(createGeometry(gbc.getGeoBoundary(product)));
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

    private PGgeometry createGeometry(Point[] geoBoundary) throws IOException {
        if (geoBoundary == null) {
            return null;
        }
        return new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(geoBoundary)}));
    }


    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVariableName,
                      String targetVariableName, int matchupIndex, final PGgeometry refPoint, final Date refTime) throws
                                                                                                                  IOException {
        final String productFilePath = observation.getDatafile().getPath();
        final RasterDataNode node;
        if (product.containsBand(sourceVariableName)) {
            node = product.getBand(sourceVariableName);
        } else {
            node = product.getTiePointGrid(sourceVariableName);
        }

        final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
        final GeoCoding geoCoding = product.getGeoCoding();
        final float lon = (float) refPoint.getGeometry().getFirstPoint().x;
        final float lat = (float) refPoint.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));
        final int[] targetOrigin = new int[]{matchupIndex, 0, 0};
        final int[] targetShape = targetVariable.getShape();
        final Rectangle rectangle = createSubsceneRectangle(pixelPos, targetShape);
        final Array array = readSubsceneData(node, dataType, targetShape, rectangle);
        try {
            targetFile.write(NetcdfFile.escapeName(targetVariableName), targetOrigin, array);
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to write to netcdf-file '" + productFilePath + "'.", e);
        }
    }

    private static Rectangle createSubsceneRectangle(final PixelPos subsceneCenter, final int[] subsceneShape) {
        final int w = subsceneShape[2];
        final int h = subsceneShape[1];
        final int x = (int) Math.floor(subsceneCenter.getX()) - w / 2;
        final int y = (int) Math.floor(subsceneCenter.getY()) - h / 2;
        return new Rectangle(x, y, w, h);
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
