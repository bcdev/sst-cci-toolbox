package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;
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

import java.awt.Rectangle;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProductIOHandler implements IOHandler {

    private final String sensorName;
    private final GeoBoundaryCalculator gbc;
    private final Map<String, Product> products = new HashMap<String, Product>();

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
    public void write(NetcdfFileWriteable file, Observation observation, VariableDescriptor variableDescriptor,
                      int matchupIndex,
                      int[] dimensionSizes, final PGgeometry refPoint, final Date refTime) throws IOException {
        final String fileLocation = observation.getDatafile().getPath();
        final Product product = getProduct(fileLocation);
        String sensorName = observation.getSensor();
        String originalVarName = variableDescriptor.getName();
        String variableName = originalVarName.replace(sensorName + ".", "");
        final Band band = product.getBand(variableName);
        if (band == null) {
            // todo - ts - log warning
            return;
        }

        final DataType type = DataTypeUtils.getNetcdfDataType(band);
        final int[] origin = createOriginArray(matchupIndex, variableDescriptor);
        final int[] shape = createShapeArray(origin.length, dimensionSizes);

        final GeoCoding geoCoding = product.getGeoCoding();
        final float lon = (float) refPoint.getGeometry().getFirstPoint().x;
        final float lat = (float) refPoint.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
        final Rectangle rectangle = createRect(dimensionSizes, shape, pixelPos);

        final Array array = createArray(band, type, shape, rectangle);
        originalVarName = NetcdfFile.escapeName(originalVarName);

        try {
            file.write(originalVarName, origin, array);
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to write to netcdf-file '" + fileLocation + "'.", e);
        }
    }

    private Rectangle createRect(final int[] dimensionSizes, final int[] shape, final PixelPos pixelPos) {
        final Rectangle rectangle;
        if (dimensionSizes.length == 0 || dimensionSizes.length == 1) {
            // write scalar
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, 1, 1);
        } else if (dimensionSizes.length == 2) {
            // write one dimension
            pixelPos.x = pixelPos.x - dimensionSizes[1] / 2;
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, shape[1], 1);
        } else if (dimensionSizes.length >= 3) {
            // write two dimensions
            pixelPos.x = pixelPos.x - dimensionSizes[1] / 2;
            pixelPos.y = pixelPos.y - dimensionSizes[2] / 2;
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, shape[1], shape[2]);
        } else {
            // cannot come here
            throw new IllegalStateException("Array size < 0.");
        }
        return rectangle;
    }

    private Array createArray(final Band band, final DataType type, final int[] shape, final Rectangle rectangle) {
        final Array array = Array.factory(type, shape);
        int index = 0;
        for (int x = rectangle.getLocation().x; x < rectangle.getWidth(); x++) {
            for (int y = rectangle.getLocation().y; y < rectangle.getHeight(); y++) {
                Object value = getValue(band, x, y);
                array.setObject(index, value);
            }
            index++;
        }
        return array;
    }

    private Object getValue(final Band band, final int x, final int y) {
        Object value = null;
        if (x < 0 || y < 0) {
            value = band.getNoDataValue();
        } else {
            switch (band.getDataType()) {
                case ProductData.TYPE_FLOAT64: {
                    value = ProductUtils.getGeophysicalSampleDouble(band, x, y, 0);
                    break;
                }
                case ProductData.TYPE_FLOAT32: {
                    value = (float) ProductUtils.getGeophysicalSampleDouble(band, x, y, 0);
                    break;
                }
                case ProductData.TYPE_INT8:
                case ProductData.TYPE_INT16:
                case ProductData.TYPE_INT32:
                case ProductData.TYPE_UINT8:
                case ProductData.TYPE_UINT16:
                case ProductData.TYPE_UINT32: {
                    value = ProductUtils.getGeophysicalSampleLong(band, x, y, 0);
                    break;
                }
            }
        }
        return value;
    }

    private Product getProduct(String fileLocation) throws IOException {
        Product product = products.get(fileLocation);
        if (product != null) {
            return product;
        }
        product = ProductIO.readProduct(fileLocation);
        products.put(fileLocation, product);
        return product;
    }


    int[] createOriginArray(int matchupIndex, VariableDescriptor variableDescriptor) {
        String dimString = variableDescriptor.getDimensions();
        final String dimensionRoles = variableDescriptor.getDimensionRoles();
        String[] dims = dimString.split(" ");
        int length = dims.length;
        final boolean addMatchup = !(dimString.contains(Constants.DIMENSION_NAME_MATCHUP) ||
                                     dimensionRoles.contains(Constants.DIMENSION_NAME_MATCHUP));
        length += addMatchup ? 1 : 0;
        final int[] origin = new int[length];
        origin[0] = matchupIndex;
        for (int i = 1; i < origin.length; i++) {
            origin[i] = 0;
        }
        return origin;
    }

    int[] createShapeArray(int length, int[] dimensionSizes) {
        Assert.argument(length == dimensionSizes.length,
                        "length == dimensionSizes.length || length == dimensionSizes.length + 1");
        int[] shape = new int[length];
        shape[0] = 1;
        System.arraycopy(dimensionSizes, 1, shape, 1, shape.length - 1);
        return shape;
    }

}
