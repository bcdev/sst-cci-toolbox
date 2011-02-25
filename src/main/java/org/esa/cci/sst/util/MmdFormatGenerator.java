package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
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

    private final PersistenceManager persistenceManager;

    public static void main(String[] args) throws IOException {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        generator.generateMmdFile("mmd.nc");
        NCdumpW.printHeader("mmd.nc", new PrintWriter(System.out));
    }

    public MmdFormatGenerator() {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME);
    }

    private void generateMmdFile(String fileName) throws IOException {
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew(fileName);

        file.addUnlimitedDimension(DIMENSION_NAME_MATCHUP);
        file.addDimension("aatsr-md-ref.cs_length", 8);
        file.addDimension("aatsr-md-ref.ui_length", 30);
        file.addDimension("aatsr-md-ref.length", 65);
        file.addDimension("aatsr.ni", 101);
        file.addDimension("aatsr.nj", 101);
        file.addDimension("avhrr.ni", 231);
        file.addDimension("avhrr.nj", 25);
        file.addDimension("metop.ni", 21);
        file.addDimension("metop.nj", 21);
        file.addDimension("metop.len_id", 11);
        file.addDimension("metop.len_filename", 65);
        file.addDimension("seviri.ni", 5);
        file.addDimension("seviri.nj", 5);
        file.addDimension("seviri.len_id", 11);
        file.addDimension("seviri.len_filename", 65);
        file.addDimension("amsre.ni", 11);
        file.addDimension("amsre.nj", 11);
        file.addDimension("tmi.ni", 11);
        file.addDimension("tmi.nj", 11);
        // todo: tie point dimensions for all sensors (rq-20110223)

        addVariables(file, Constants.SENSOR_NAME_AATSR_MD);
        addInsituDataHistories(file);

        addObservationTime(file, Constants.SENSOR_NAME_AATSR);
        addLsMask(file, Constants.SENSOR_NAME_AATSR);
        addVariables(file, Constants.SENSOR_NAME_AATSR);
        addNwpData(file, Constants.SENSOR_NAME_AATSR);

        addObservationTime(file, Constants.SENSOR_NAME_METOP);
        addLsMask(file, Constants.SENSOR_NAME_METOP);
        addVariables(file, Constants.SENSOR_NAME_METOP);
        addNwpData(file, Constants.SENSOR_NAME_METOP);

        addObservationTime(file, Constants.SENSOR_NAME_SEVIRI);
        addLsMask(file, Constants.SENSOR_NAME_SEVIRI);
        addVariables(file, Constants.SENSOR_NAME_SEVIRI);
        addNwpData(file, Constants.SENSOR_NAME_SEVIRI);

        addVariables(file, Constants.SENSOR_NAME_AVHRR);
        addLsMask(file, Constants.SENSOR_NAME_AVHRR);
        addObservationTime(file, Constants.SENSOR_NAME_AVHRR);
        addNwpData(file, Constants.SENSOR_NAME_AVHRR);

        addObservationTime(file, Constants.SENSOR_NAME_AMSRE);
        addLsMask(file, Constants.SENSOR_NAME_AMSRE);
        addVariables(file, Constants.SENSOR_NAME_AMSRE);
        addNwpData(file, Constants.SENSOR_NAME_AMSRE);

        addObservationTime(file, Constants.SENSOR_NAME_TMI);
        addLsMask(file, Constants.SENSOR_NAME_TMI);
        addVariables(file, Constants.SENSOR_NAME_TMI);
        addNwpData(file, Constants.SENSOR_NAME_TMI);

        addGlobalAttributes(file);

        file.create();
        file.close();
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
