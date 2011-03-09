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
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.util.MmdFormatGenerator;
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

public class ProductObservationReader implements ObservationReader {

    private final String sensorName;
    private final GeoBoundaryCalculator gbc;
    private final Map<String, Product> products = new HashMap<String, Product>();

    private DataFile dataFile;
    private Product product;


    public ProductObservationReader(String sensorName, GeoBoundaryCalculator gbc) {
        this.sensorName = sensorName;
        this.gbc = gbc;
    }

    @Override
    public final void init(DataFile dataFileEntry) throws IOException {
        final Product product = ProductIO.readProduct(dataFileEntry.getPath());
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", dataFileEntry.getPath()));
        }
        this.product = product;
        this.dataFile = dataFileEntry;
    }

    @Override
    public final void close() throws IOException {
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
        if (product == null) {
            return null;
        }

        final RelatedObservation observation;
        // TODO move distinction to reader level instead
        if (gbc instanceof DefaultGeoBoundaryCalculator) {
            observation = new RelatedObservation();
            observation.setLocation(createGeometry(gbc.getGeoBoundary(product)));
        } else {
            observation = new RelatedObservation();
        }

        observation.setDatafile(dataFile);
        observation.setRecordNo(0);
        observation.setSensor(sensorName);
        observation.setTime(getCenterTimeAsDate());

        return observation;
    }

    @Override
    public Variable[] getVariables() throws IOException {
        final ArrayList<Variable> variableList = new ArrayList<Variable>();
        for (RasterDataNode node : product.getTiePointGrids()) {
            final Variable variable = new Variable();
            variable.setName(String.format("%s.%s", sensorName, node.getName()));
            variable.setDataSchema(dataFile.getDataSchema());
            final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
            variable.setType(dataType.name());
            variable.setDimensions("ni nj");
            variable.setDimensionRoles("ni nj");
            if (node.isScalingApplied()) {
                variable.setAddOffset(node.getScalingOffset());
                variable.setScaleFactor(node.getScalingFactor());
            }
            if (node.isNoDataValueUsed()) {
                variable.setFillValue(node.getNoDataValue());
            }
            variable.setUnits(node.getUnit());
            variableList.add(variable);
        }
        for (RasterDataNode node : product.getBands()) {
            final Variable variable = new Variable();
            variable.setName(String.format("%s.%s", sensorName, node.getName()));
            variable.setDataSchema(dataFile.getDataSchema());
            final DataType dataType = DataTypeUtils.getNetcdfDataType(node);
            variable.setType(dataType.name());
            variable.setDimensions("ni nj");
            variable.setDimensionRoles("ni nj");
            if (node.isScalingApplied()) {
                variable.setAddOffset(node.getScalingOffset());
                variable.setScaleFactor(node.getScalingFactor());
            }
            if (node.isNoDataValueUsed()) {
                variable.setFillValue(node.getNoDataValue());
            }
            final String units = node.getUnit();
            if (units != null && !units.isEmpty()) {
                variable.setUnits(units);
            }
            variableList.add(variable);
        }
        return variableList.toArray(new Variable[variableList.size()]);
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
    public void write(Observation observation, Variable variable, NetcdfFileWriteable file, int matchupIndex,
                      int[] dimensionSizes, final PGgeometry point) throws IOException {
        // todo - complete at least for aai, perhaps for sea ice as well (wait for feedback)
        final String fileLocation = observation.getDatafile().getPath();
        final Product product = getProduct(fileLocation);
        String sensorName = observation.getSensor();
        String originalVarName = variable.getName();
        String variableName = originalVarName.replace(sensorName + ".", "");
        final Band band = product.getBand(variableName);
        if (band == null) {
            // todo - ts - log warning
            return;
        }

        final DataType type = DataTypeUtils.getNetcdfDataType(band);
        final int[] origin = createOriginArray(matchupIndex, variable);
        final int[] shape = createShapeArray(origin.length, dimensionSizes);

        final Rectangle rectangle;
        final GeoCoding geoCoding = product.getGeoCoding();
        final float lon = (float) point.getGeometry().getFirstPoint().x;
        final float lat = (float) point.getGeometry().getFirstPoint().y;
        final GeoPos geoPos = new GeoPos(lat, lon);
        PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);

        if (dimensionSizes.length == 0 || dimensionSizes.length == 1) {
            // write scalar
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, 1, 1);
        } else if (dimensionSizes.length == 2) {
            // write one dimension
            pixelPos.x = pixelPos.x - dimensionSizes[1] / 2;
            correctPixelPosAndShape(shape, pixelPos);
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, shape[1], 1);
        } else if (dimensionSizes.length >= 3) {
            // write two dimensions
            pixelPos.x = pixelPos.x - dimensionSizes[1] / 2;
            pixelPos.y = pixelPos.y - dimensionSizes[2] / 2;
            correctPixelPosAndShape(shape, pixelPos);
            rectangle = new Rectangle((int) pixelPos.x, (int) pixelPos.y, shape[1], shape[2]);
        } else {
            // cannot come here
            throw new IllegalStateException("Array size < 0.");
        }

        final Array array = createArray(band, type, shape, rectangle);
        originalVarName = NetcdfFile.escapeName(originalVarName);

        try {
            file.write(originalVarName, origin, array);
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to write to netcdf-file '" + fileLocation + "'.", e);
        }
    }

    private void correctPixelPosAndShape(final int[] shape, final PixelPos pixelPos) {
        if (pixelPos.x < 0) {
            shape[1] = (int) (shape[1] + 2 * pixelPos.x);
            pixelPos.x = 0;
        }
        if (pixelPos.y < 0) {
            shape[2] = (int) (shape[2] + 2 * pixelPos.y);
            pixelPos.y = 0;
        }
    }

    private Array createArray(final Band band, final DataType type, final int[] shape, final Rectangle rectangle) {
        final Array array = Array.factory(type, shape);
        for (int x = rectangle.getLocation().x; x < rectangle.getWidth(); x++) {
            for (int y = rectangle.getLocation().y; y < rectangle.getHeight(); y++) {
                Object value = null;
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
                    }
                }
                array.setObject(computeIndex(x, y, (int) (rectangle.getHeight() - 1)), value);
            }
        }
        return array;
    }

    static int computeIndex(final int x, final int y, final int maxY) {
        return x * (maxY + 1) + y;
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


    int[] createOriginArray(int matchupIndex, Variable variable) {
        String dimString = variable.getDimensions();
        final String dimensionRoles = variable.getDimensionRoles();
        String[] dims = dimString.split(" ");
        int length = dims.length;
        final boolean addMatchup = !(dimString.contains(MmdFormatGenerator.DIMENSION_NAME_MATCHUP) ||
                                     dimensionRoles.contains(MmdFormatGenerator.DIMENSION_NAME_MATCHUP));
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
