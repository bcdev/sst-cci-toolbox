package org.esa.cci.sst.reader;

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Variable;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;

public class ProductObservationReader implements ObservationReader {

    private final String sensorName;
    private final GeoBoundaryCalculator gbc;

    private DataFile dataFile;
    private Product product;

    public ProductObservationReader(String sensorName, GeoBoundaryCalculator gbc) {
        this.sensorName = sensorName;
        this.gbc = gbc;
    }

    @Override
    public final void init(File observationFile, DataFile dataFileEntry) throws IOException {
        final Product product = ProductIO.readProduct(observationFile);
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", observationFile.getPath()));
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
    public final long getTime(int recordNo) throws IOException, InvalidRangeException {
        if (product == null) {
            throw new IOException("Unable to get start time.");
        }
        return getCenterTimeAsDate().getTime();
    }

    @Override
    public final GlobalObservation readObservation(int recordNo) throws IOException, InvalidRangeException {
        if (product == null) {
            return null;
        }

        final GlobalObservation observation;
        // TODO move distinction to reader level instead
        if (gbc instanceof DefaultGeoBoundaryCalculator) {
            observation = new RelatedObservation();
            ((RelatedObservation) observation).setLocation(createGeometry(gbc.getGeoBoundary(product)));
        } else {
            observation = new GlobalObservation();
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
}
