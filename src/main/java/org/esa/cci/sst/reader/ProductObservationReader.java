package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.util.MmdFormatGenerator;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

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
    public final RelatedObservation readObservation(int recordNo) throws IOException {
        if (product == null) {
            return null;
        }

        final RelatedObservation observation;
        // TODO move distinction to reader level instead
        if (gbc instanceof DefaultGeoBoundaryCalculator) {
            observation = new RelatedObservation();
            ((RelatedObservation) observation).setLocation(createGeometry(gbc.getGeoBoundary(product)));
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
                      int[] dimensionSizes) throws IOException {
        final String fileLocation = observation.getDatafile().getPath();
        final Product product = getProduct(fileLocation);
        final GeoCoding geoCoding = product.getGeoCoding();
        String sensorName = observation.getSensor();
        String originalVarName = variable.getName();
        String variableName = originalVarName.replace(sensorName + ".", "");
        final Band band = product.getBand(variableName);
        if (band == null) {
            return;
        }

        final DataType type = DataTypeUtils.getNetcdfDataType(band);
        final int[] origin = createOriginArray(matchupIndex, variable);
        final int[] shape = createShapeArray(origin.length, dimensionSizes);
//        final Array array = Array.factory(type, shape, data);
//        array.setObject(0, sample);
        originalVarName = NetcdfFile.escapeName(originalVarName);
//        file.write(originalVarName, origin, array);
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
        Assert.argument(length == dimensionSizes.length + 1);
        int[] shape = new int[length];
        shape[0] = 1;
        System.arraycopy(dimensionSizes, 0, shape, 1, shape.length - 1);
        return shape;
    }

}
