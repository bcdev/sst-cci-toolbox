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
import java.util.List;

public class MmdFormatGenerator {

    private final PersistenceManager persistenceManager;

    public static void main(String[] args) throws IOException {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        generator.generateMmdFormat();
    }

    public MmdFormatGenerator() {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME);
    }

    private void generateMmdFormat() throws IOException {
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew("mmd.nc");
        file.addUnlimitedDimension("match_up");

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

        addVariables(file, Constants.SENSOR_NAME_AATSR_REFERENCE);

        addObservationTime(file, Constants.SENSOR_NAME_AATSR);
        addVariables(file, Constants.SENSOR_NAME_AATSR);

        addObservationTime(file, Constants.SENSOR_NAME_METOP);
        addVariables(file, Constants.SENSOR_NAME_METOP);

        addObservationTime(file, Constants.SENSOR_NAME_SEVIRI);
        addVariables(file, Constants.SENSOR_NAME_SEVIRI);

        addVariables(file, Constants.SENSOR_NAME_AVHRR);
        addObservationTime(file, Constants.SENSOR_NAME_AVHRR);

        addObservationTime(file, Constants.SENSOR_NAME_AMSRE);
        addVariables(file, Constants.SENSOR_NAME_AMSRE);

        addObservationTime(file, Constants.SENSOR_NAME_TMI);
        addVariables(file, Constants.SENSOR_NAME_TMI);

        file.create();
        file.close();
        NCdumpW.printHeader("mmd.nc", new PrintWriter(System.out));
    }

    private void addObservationTime(NetcdfFileWriteable file, String sensorName) {
        file.addVariable(file.getRootGroup(), "observation_time", DataType.DOUBLE,
                         String.format("match_up %s.ni", sensorName));
    }

    private void addMdVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final List<Variable> resultList = query.getResultList();
        for (Variable variable : resultList) {
            final String[] dimensionNames = variable.getDimensions().split(" ");
            final String[] dimensionRoles = variable.getDimensionRoles().split(" ");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dimensionRoles.length; i++) {
                final String dimensionName = dimensionNames[i];
                final String dimensionRole = dimensionRoles[i];
                if (i != 0) {
                    sb.append(" ");
                }
                if (!"match_up".equals(dimensionRole)) {
                    sb.append(sensorName);
                    sb.append(".");
                }
                if (!dimensionRole.contains("length")) {
                    sb.append(dimensionRole);
                } else {
                    sb.append(dimensionName);
                }
            }
            if (!sb.toString().contains("match_up")) {
                sb.insert(0, "match_up" + " ");
            }
            try {
                final ucar.nc2.Variable ncVar =
                        file.addVariable(file.getRootGroup(), variable.getName(), DataType.valueOf(variable.getType()),
                                         sb.toString());
                addAttribute(ncVar, "add_offset", variable.getAddOffset(), DataType.FLOAT);
                addAttribute(ncVar, "scale_factor", variable.getScaleFactor(), DataType.FLOAT);
                addAttribute(ncVar, "_FillValue", variable.getFillValue(), ncVar.getDataType());
                if (variable.getUnits() != null) {
                    ncVar.addAttribute(new Attribute("units", variable.getUnits()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final List<Variable> resultList = query.getResultList();
        for (Variable variable : resultList) {
            final String[] dimensionNames = variable.getDimensions().split(" ");
            final String[] dimensionRoles = variable.getDimensionRoles().split(" ");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dimensionRoles.length; i++) {
                final String dimensionName = dimensionNames[i];
                final String dimensionRole = dimensionRoles[i];
                if (i != 0) {
                    sb.append(" ");
                }
                if (!"match_up".equals(dimensionRole)) {
                    sb.append(sensorName);
                    sb.append(".");
                }
                if (!"length".equals(dimensionRole)) {
                    sb.append(dimensionRole);
                } else {
                    sb.append(dimensionName);
                }
            }
            if (!sb.toString().contains("match_up")) {
                sb.insert(0, "match_up ");
            }
            try {
                final ucar.nc2.Variable ncVar =
                        file.addVariable(file.getRootGroup(), variable.getName(), DataType.valueOf(variable.getType()),
                                         sb.toString());
                addAttribute(ncVar, "add_offset", variable.getAddOffset(), DataType.FLOAT);
                addAttribute(ncVar, "scale_factor", variable.getScaleFactor(), DataType.FLOAT);
                addAttribute(ncVar, "_FillValue", variable.getFillValue(), ncVar.getDataType());
                if (variable.getUnits() != null) {
                    ncVar.addAttribute(new Attribute("units", variable.getUnits()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addAttribute(ucar.nc2.Variable ncVar, String attrName, Number attrValue, DataType dataType) {
        if (attrValue != null) {
            switch (dataType) {
                case BYTE:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.byteValue()));
                    break;
                case SHORT:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.shortValue()));
                    break;
                case INT:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.intValue()));
                    break;
                case LONG:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.longValue()));
                    break;
                case FLOAT:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.floatValue()));
                    break;
                case DOUBLE:
                    ncVar.addAttribute(new Attribute(attrName, attrValue.doubleValue()));
                    break;
            }
        }
    }

    private Query createMatchupCountQuery() {
        return persistenceManager.createQuery("select count(m) from Matchup m");
    }

    private Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(
                String.format("select v from Variable v where v.name like '%s.%%'", sensorName));
    }
}
