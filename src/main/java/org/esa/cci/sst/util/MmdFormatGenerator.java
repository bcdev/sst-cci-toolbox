package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
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

    private final PersistenceManager persistenceManager;

    public static void main(String[] args) throws Exception {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        NetcdfFileWriteable mmdFile = null;
        try {
            mmdFile = generator.createMmdFile("mmd.nc");
        } finally {
            if (mmdFile != null) {
                mmdFile.close();
            }
        }
    }

    public MmdFormatGenerator() {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME);
    }

    private NetcdfFileWriteable createMmdFile(String fileName) throws Exception {
        final NetcdfFileWriteable mmdFile = NetcdfFileWriteable.createNew(fileName);

        mmdFile.addUnlimitedDimension(DIMENSION_NAME_MATCHUP);
        mmdFile.addDimension("aatsr-md.cs_length", 8);
        mmdFile.addDimension("aatsr-md.ui_length", 30);
        mmdFile.addDimension("aatsr-md.length", 65);
        mmdFile.addDimension("metop.ni", 21);
        mmdFile.addDimension("metop.nj", 21);
        mmdFile.addDimension("metop.len_id", 11);
        mmdFile.addDimension("metop.len_filename", 65);
        mmdFile.addDimension("seviri.ni", 5);
        mmdFile.addDimension("seviri.nj", 5);
        mmdFile.addDimension("seviri.len_id", 11);
        mmdFile.addDimension("seviri.len_filename", 65);
        mmdFile.addDimension("aatsr.ni", 101);
        mmdFile.addDimension("aatsr.nj", 101);
        mmdFile.addDimension("avhrr.ni", 231);
        mmdFile.addDimension("avhrr.nj", 25);
        mmdFile.addDimension("amsre.ni", 11);
        mmdFile.addDimension("amsre.nj", 11);
        mmdFile.addDimension("tmi.ni", 11);
        mmdFile.addDimension("tmi.nj", 11);
        // todo: tie point dimensions for all sensors (rq-20110223)

        addVariables(mmdFile, Constants.SENSOR_NAME_AATSR_MD);
        addInsituDataHistories(mmdFile);

        addObservationTime(mmdFile, Constants.SENSOR_NAME_AATSR);
        addLsMask(mmdFile, Constants.SENSOR_NAME_AATSR);
        addVariables(mmdFile, Constants.SENSOR_NAME_AATSR);
        addNwpData(mmdFile, Constants.SENSOR_NAME_AATSR);

        addObservationTime(mmdFile, Constants.SENSOR_NAME_METOP);
        addLsMask(mmdFile, Constants.SENSOR_NAME_METOP);
        addVariables(mmdFile, Constants.SENSOR_NAME_METOP);
        addNwpData(mmdFile, Constants.SENSOR_NAME_METOP);

        addObservationTime(mmdFile, Constants.SENSOR_NAME_SEVIRI);
        addLsMask(mmdFile, Constants.SENSOR_NAME_SEVIRI);
        addVariables(mmdFile, Constants.SENSOR_NAME_SEVIRI);
        addNwpData(mmdFile, Constants.SENSOR_NAME_SEVIRI);

        addVariables(mmdFile, Constants.SENSOR_NAME_AVHRR);
        addLsMask(mmdFile, Constants.SENSOR_NAME_AVHRR);
        addObservationTime(mmdFile, Constants.SENSOR_NAME_AVHRR);
        addNwpData(mmdFile, Constants.SENSOR_NAME_AVHRR);

        addObservationTime(mmdFile, Constants.SENSOR_NAME_AMSRE);
        addLsMask(mmdFile, Constants.SENSOR_NAME_AMSRE);
        addVariables(mmdFile, Constants.SENSOR_NAME_AMSRE);
        addNwpData(mmdFile, Constants.SENSOR_NAME_AMSRE);

        addObservationTime(mmdFile, Constants.SENSOR_NAME_TMI);
        addLsMask(mmdFile, Constants.SENSOR_NAME_TMI);
        addVariables(mmdFile, Constants.SENSOR_NAME_TMI);
        addNwpData(mmdFile, Constants.SENSOR_NAME_TMI);

        addContent(mmdFile);
        addGlobalAttributes(mmdFile);

        mmdFile.create();
        return mmdFile;
    }

    @SuppressWarnings({"unchecked"})
    void addContent(NetcdfFileWriteable file) throws Exception {
        try {
            // open database
            persistenceManager.transaction();

            // clear coincidences as they are computed from scratch
            Query getAllMatchupsQuery = persistenceManager.createQuery(ALL_MATCHUPS_QUERY);
            final List<Matchup> resultList = getAllMatchupsQuery.getResultList();

            for (Matchup matchup : resultList) {
//                final ucar.nc2.Variable mId = file.findVariable("mid");
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final List<Coincidence> coincidences = matchup.getCoincidences();
                for (Coincidence coincidence : coincidences) {
                    final GlobalObservation observation = coincidence.getObservation();
                    // todo - ts, mb - get file locations, get reader, read data, put into netcdf-file
                }
            }

            // make changes in database
            persistenceManager.commit();
            // do not make any change in case of errors
        } catch (Exception e) {
            persistenceManager.rollback();
            throw e;
        }
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
