package org.esa.cci.sst.util;

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ProductUtils;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;

public class MmdFormatGenerator {

    public static final String DIMENSION_NAME_MATCHUP = "match_up";
    public static final String DIMENSION_ROLE_MATCHUP = "match_up";
    public static final String DIMENSION_ROLE_LENGTH = "length";

    private static final String ALL_MATCHUPS_QUERY =
            "select m"
            + " from Matchup m"
            + " order by m.id";

    private static final String VARIABLES_BY_DATASCHEMA_ID_QUERY =
            "select v"
            + " from Variable v"
            + " where v.dataSchema.id = %d";

    private final PersistenceManager persistenceManager;

    public static void main(String[] args) throws Exception {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        final NetcdfFileWriteable file = generator.generateMmdFile("mmd.nc");
        generator.addContent(file);
        file.close();
        NCdumpW.printHeader("mmd.nc", new PrintWriter(System.out));
    }

    public MmdFormatGenerator() {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME);
    }

    NetcdfFileWriteable generateMmdFile(String fileName) throws Exception {
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew(fileName);

        file.addUnlimitedDimension(DIMENSION_NAME_MATCHUP);
        file.addDimension("aatsr-md.cs_length", 8);
        file.addDimension("aatsr-md.ui_length", 30);
        file.addDimension("aatsr-md.length", 65);
        file.addDimension("metop.ni", 21);
        file.addDimension("metop.nj", 21);
        file.addDimension("metop.len_id", 11);
        file.addDimension("metop.len_filename", 65);
        file.addDimension("seviri.ni", 5);
        file.addDimension("seviri.nj", 5);
        file.addDimension("seviri.len_id", 11);
        file.addDimension("seviri.len_filename", 65);
        file.addDimension("aatsr.ni", 101);
        file.addDimension("aatsr.nj", 101);
        file.addDimension("avhrr.ni", 231);
        file.addDimension("avhrr.nj", 25);
        file.addDimension("amsre.ni", 11);
        file.addDimension("amsre.nj", 11);
        file.addDimension("tmi.ni", 11);
        file.addDimension("tmi.nj", 11);
        // todo - clarify if this is ok (in each case the maximum dimension value of both grids, cp. Sea Ice Product Manual)
        file.addDimension("seaice.ni", 790);
        file.addDimension("seaice.nj", 1120);
        // todo: tie point dimensions for all sensors (rq-20110223)

        addVariables(file, Constants.SENSOR_NAME_AATSR_MD);
        addInsituDataHistories(file);

        for (String sensorName : Constants.SENSOR_NAMES) {
            addObservationTime(file, sensorName);
            addLsMask(file, sensorName);
            addVariables(file, sensorName);
            addNwpData(file, sensorName);
        }

        addGlobalAttributes(file);
        file.create();

        return file;
    }

    @SuppressWarnings({"unchecked"})
    void addContent(NetcdfFileWriteable file) throws Exception {
        // open database
        persistenceManager.transaction();

        Query getAllMatchupsQuery = persistenceManager.createQuery(ALL_MATCHUPS_QUERY);
        final List<Matchup> resultList = getAllMatchupsQuery.getResultList();

        for (int i = 0; i < resultList.size(); i++) {
            Matchup matchup = resultList.get(i);
            final ReferenceObservation referenceObservation = matchup.getRefObs();
            final List<Coincidence> coincidences = matchup.getCoincidences();
            final Point referencePoint = referenceObservation.getPoint().getGeometry().getPoint(0);
            final PixelPos referencePixelPos = new PixelPos();
            for (Coincidence coincidence : coincidences) {
                writeCoincidence(file, referencePoint, referencePixelPos, coincidence, i);
            }
        }
        persistenceManager.commit();
    }

    @SuppressWarnings({"unchecked"})
    private void writeCoincidence(NetcdfFileWriteable file, Point referencePoint, PixelPos referencePixelPos,
                                  Coincidence coincidence, int matchupIndex) throws IOException, InvalidRangeException {
        // todo - ts, mb - get file locations, get reader, read data, put into netcdf-file
        final GlobalObservation observation = coincidence.getObservation();
        final String fileLocation = observation.getDatafile().getPath();
        final Product product = ProductIO.readProduct(fileLocation);
        final GeoCoding geoCoding = product.getGeoCoding();
        geoCoding.getPixelPos(new GeoPos((float) referencePoint.x, (float) referencePoint.y),
                              referencePixelPos);
        final int dataschemaId = observation.getDatafile().getDataSchema().getId();
        final Query getVariablesByDataschemaId = persistenceManager.createQuery(
                String.format(VARIABLES_BY_DATASCHEMA_ID_QUERY, dataschemaId));
        final List<Variable> variables = getVariablesByDataschemaId.getResultList();
        String sensorName = observation.getSensor();
        for (Variable variable : variables) {
            String originalVarName = variable.getName();
            String variableName = originalVarName.replace(sensorName + ".", "");
            final Band band = product.getBand(variableName);
            if (band == null) {
                continue;
            }
            final Object sample = getSample(referencePixelPos, band);
            final DataType type = DataTypeUtils.getNetcdfDataType(band);
            // todo - consider growing origins
            final int[] origin = createOriginArray(matchupIndex, variable);
            final int[] shape = createShapeArray(origin.length);
            final Array array = Array.factory(type, shape);
            array.setObject(0, sample);
            originalVarName = NetcdfFile.escapeName(originalVarName);
            file.write(originalVarName, origin, array);
        }
    }

    private Object getSample(PixelPos referencePixelPos, Band band) {
        final double sampleDouble = ProductUtils.getGeophysicalSampleDouble(band, (int) referencePixelPos.x,
                                                                            (int) referencePixelPos.y, 0);
        switch (band.getDataType()) {
            case ProductData.TYPE_INT8: {
                return (byte) sampleDouble;
            }
            case ProductData.TYPE_INT16: {
                return (short) sampleDouble;
            }
            case ProductData.TYPE_INT32: {
                return (int) sampleDouble;
            }
            case ProductData.TYPE_FLOAT32: {
                return (float) sampleDouble;
            }
            case ProductData.TYPE_FLOAT64: {
                return sampleDouble;
            }
        }
        throw new IllegalStateException("Band has wrong datatype '" + band.getData().getTypeString() + "'.");
    }

    int[] createOriginArray(int matchupIndex, Variable variable) {
        String dimString = variable.getDimensions();
        final String dimensionRoles = variable.getDimensionRoles();
        String[] dims = dimString.split(" ");
        int length = dims.length;
        final boolean addMatchup = !(dimString.contains(DIMENSION_NAME_MATCHUP) ||
                                     dimensionRoles.contains(DIMENSION_NAME_MATCHUP));
        length += addMatchup ? 1 : 0;
        final int[] origin = new int[length];
        origin[0] = matchupIndex;
        for (int i = 1; i < origin.length; i++) {
            origin[i] = 0;
        }
        return origin;
    }

    private int[] createShapeArray(int size) {
        final int[] shape = new int[size];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = 1;
        }
        return shape;
    }

    private void addGlobalAttributes(NetcdfFileWriteable file) {
        file.addGlobalAttribute("title", "SST CCI multi-sensor match-up dataset (MMD) template");
        file.addGlobalAttribute("institution", "Brockmann Consult");
        file.addGlobalAttribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)");
        file.addGlobalAttribute("creation_date", Calendar.getInstance().getTime().toString());
        file.addGlobalAttribute("total_number_of_matchups", 0);
    }

    private void addInsituDataHistories(NetcdfFileWriteable file) {
        // todo: add in-situ data histories (rq-20110223)
    }

    private void addNwpData(NetcdfFileWriteable file, String sensorName) {
        // todo: add NWP data (rq-20110223)
    }

    private void addObservationTime(NetcdfFileWriteable file, String sensorName) {
        final ucar.nc2.Variable time = file.addVariable(file.getRootGroup(),
                                                        String.format("%s.observation_time", sensorName),
                                                        DataType.DOUBLE,
                                                        String.format("match_up %s.ni", sensorName));
        addAttribute(time, "units", "Julian Date");
    }

    private void addLsMask(NetcdfFileWriteable file, String sensorName) {
        file.addVariable(file.getRootGroup(),
                         String.format("%s.land_sea_mask", sensorName),
                         DataType.BYTE,
                         String.format("match_up %s.ni %s.nj", sensorName, sensorName));
    }

    private void addVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final List<Variable> resultList = query.getResultList();
        for (final Variable var : resultList) {
            addVariable(file, var, createDimensionString(var, sensorName));
        }
    }

    private String createDimensionString(Variable var, String sensorName) {
        final String[] dimensionNames = var.getDimensions().split(" ");
        final String[] dimensionRoles = var.getDimensionRoles().split(" ");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dimensionRoles.length; i++) {
            final String dimensionName = dimensionNames[i];
            final String dimensionRole = dimensionRoles[i];
            if (i != 0) {
                sb.append(" ");
            }
            if (!DIMENSION_ROLE_MATCHUP.equals(dimensionRole)) {
                sb.append(sensorName);
                sb.append(".");
            }
            if (!DIMENSION_ROLE_LENGTH.equals(dimensionRole)) {
                sb.append(dimensionRole);
            } else {
                sb.append(dimensionName);
            }
        }
        if (!sb.toString().contains(DIMENSION_NAME_MATCHUP)) {
            sb.insert(0, DIMENSION_NAME_MATCHUP + " ");
        }
        return sb.toString();
    }

    private void addVariable(NetcdfFileWriteable file, Variable var, String dims) {
        final ucar.nc2.Variable v = file.addVariable(file.getRootGroup(),
                                                     var.getName(),
                                                     DataType.valueOf(var.getType()),
                                                     dims);
        addAttribute(v, "standard_name", var.getStandardName());
        addAttribute(v, "units", var.getUnits());
        addAttribute(v, "add_offset", var.getAddOffset(), DataType.FLOAT);
        addAttribute(v, "scale_factor", var.getScaleFactor(), DataType.FLOAT);
        addAttribute(v, "_FillValue", var.getFillValue(), v.getDataType());
    }

    private void addAttribute(ucar.nc2.Variable v, String attrName, String attrValue) {
        if (attrValue != null) {
            v.addAttribute(new Attribute(attrName, attrValue));
        }
    }

    private void addAttribute(ucar.nc2.Variable v, String attrName, Number attrValue, DataType attrType) {
        if (attrValue != null) {
            switch (attrType) {
                case BYTE:
                    v.addAttribute(new Attribute(attrName, attrValue.byteValue()));
                    break;
                case SHORT:
                    v.addAttribute(new Attribute(attrName, attrValue.shortValue()));
                    break;
                case INT:
                    v.addAttribute(new Attribute(attrName, attrValue.intValue()));
                    break;
                case FLOAT:
                    v.addAttribute(new Attribute(attrName, attrValue.floatValue()));
                    break;
                case DOUBLE:
                    v.addAttribute(new Attribute(attrName, attrValue.doubleValue()));
                    break;
                default:
                    throw new IllegalArgumentException(
                            MessageFormat.format("Attribute type ''{0}'' is not supported", attrType.toString()));
            }
        }
    }

    private Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(
                String.format("select v from Variable v where v.name like '%s.%%' order by v.name", sensorName));
    }
}
