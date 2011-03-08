package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.ObservationReader;
import org.esa.cci.sst.reader.ReaderFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.esa.cci.sst.SensorName.*;

public class MmdFormatGenerator {

    public static final String DIMENSION_NAME_MATCHUP = "match_up";
    public static final String DIMENSION_ROLE_MATCHUP = "match_up";
    public static final String DIMENSION_ROLE_LENGTH = "length";

    public static final int AATSR_MD_CS_LENGTH = 8;
    public static final int AATSR_MD_UI_LENGTH = 30;
    public static final int AATSR_MD_LENGTH = 65;
    public static final int METOP_LENGTH = 21;
    public static final int METOP_LEN_ID = 11;
    public static final int METOP_LEN_FILENAME = 65;
    public static final int SEVIRI_LENGTH = 5;
    public static final int SEVIRI_LEN_ID = 11;
    public static final int SEVIRI_LEN_FILENAME = 65;
    public static final int AATSR_LENGTH = 101;
    public static final int AVHRR_WIDTH = 231;
    public static final int AVHRR_HEIGHT = 25;
    public static final int AMSRE_LENGTH = 11;
    public static final int TMI_LENGTH = 11;
    public static final int SEA_ICE_LENGTH = 11;

    private static final String COUNT_MATCHUPS_QUERY =
            "select count( m ) "
            + " from Matchup m";

    private static final String ALL_MATCHUPS_QUERY =
            "select m"
            + " from Matchup m"
            + " order by m.id";

    private static final String VARIABLES_BY_DATASCHEMA_ID_QUERY =
            "select v"
            + " from Variable v"
            + " where v.dataSchema.id = %d";

    private final PersistenceManager persistenceManager;

    private final Map<String, ObservationReader> readers = new HashMap<String, ObservationReader>();

    private final Map<String, Integer> dimensionCountMap = new HashMap<String, Integer>(17);

    private final Map<String, String> varDimensionMap = new HashMap<String, String>(61);

    public static void main(String[] args) throws Exception {
        NetcdfFileWriteable file = null;

        try {
            final Properties properties = new Properties();
            properties.load(new FileInputStream("mms-test.properties"));
            final MmdFormatGenerator generator = new MmdFormatGenerator(properties);
            file = generator.generateMmdFile("mmd.nc");
            generator.addContent(file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                file.close();
            }
        }
//        NCdumpW.printHeader("mmd.nc", new PrintWriter(System.out));
    }

    public MmdFormatGenerator(Properties properties) {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, properties);
        dimensionCountMap.put("aatsr-md.cs_length", AATSR_MD_CS_LENGTH);
        dimensionCountMap.put("aatsr-md.ui_length", AATSR_MD_UI_LENGTH);
        dimensionCountMap.put("aatsr-md.length", AATSR_MD_LENGTH);
        dimensionCountMap.put("metop.ni", METOP_LENGTH);
        dimensionCountMap.put("metop.nj", METOP_LENGTH);
        dimensionCountMap.put("metop.len_id", METOP_LEN_ID);
        dimensionCountMap.put("metop.len_filename", METOP_LEN_FILENAME);
        dimensionCountMap.put("seviri.ni", SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.nj", SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.len_id", SEVIRI_LEN_ID);
        dimensionCountMap.put("seviri.len_filename", SEVIRI_LEN_FILENAME);
        dimensionCountMap.put("aatsr.ni", AATSR_LENGTH);
        dimensionCountMap.put("aatsr.nj", AATSR_LENGTH);
        dimensionCountMap.put("avhrr.ni", AVHRR_WIDTH);
        dimensionCountMap.put("avhrr.nj", AVHRR_HEIGHT);
        dimensionCountMap.put("amsre.ni", AMSRE_LENGTH);
        dimensionCountMap.put("amsre.nj", AMSRE_LENGTH);
        dimensionCountMap.put("tmi.ni", TMI_LENGTH);
        dimensionCountMap.put("tmi.nj", TMI_LENGTH);
        // todo - ask gary about dimension sizes for sea ice
        dimensionCountMap.put("seaice.ni", SEA_ICE_LENGTH);
        dimensionCountMap.put("seaice.nj", SEA_ICE_LENGTH);
    }

    public void close() {
        for (ObservationReader observationReader : readers.values()) {
            try {
                observationReader.close();
            } catch (IOException ignore) {
                // ok
            }
        }
    }

    NetcdfFileWriteable generateMmdFile(String fileName) throws Exception {
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew(fileName, true);

//        file.addDimension(DIMENSION_NAME_MATCHUP, 1);
        file.addDimension(DIMENSION_NAME_MATCHUP, getMatchupCount());

        for (String dimensionName : dimensionCountMap.keySet()) {
            file.addDimension(dimensionName, dimensionCountMap.get(dimensionName));
        }
        // todo: tie point dimensions for all sensors (rq-20110223)

        addVariables(file, SENSOR_NAME_AATSR_MD.getSensor());
        addInsituDataHistories(file);

        for (SensorName sensorName : SensorName.values()) {
            if (!SENSOR_NAME_AATSR_MD.getSensor().equalsIgnoreCase(sensorName.getSensor())) {
                addObservationTime(file, sensorName.getSensor());
                addLsMask(file, sensorName.getSensor());
                addNwpData(file, sensorName.getSensor());
            }
            addVariables(file, sensorName.getSensor());
        }

        file.addVariable("mId", DataType.INT, DIMENSION_NAME_MATCHUP);
        file.setLargeFile(true);
        addGlobalAttributes(file);
        file.create();

        return file;
    }

    @SuppressWarnings({"unchecked"})
    void addContent(NetcdfFileWriteable file) throws Exception {
        // open database
        persistenceManager.transaction();
        try {

            Query getAllMatchupsQuery = persistenceManager.createQuery(ALL_MATCHUPS_QUERY);
            final List<Matchup> resultList = getAllMatchupsQuery.getResultList();
            int matchupCount = resultList.size();

            for (int matchupIndex = 0; matchupIndex < getMatchupCount(); matchupIndex++) {
                Matchup matchup = resultList.get(matchupIndex);
                final int matchupId = matchup.getId();
                System.out.println("Writing matchup '" + matchupId + "' (" + matchupIndex + "/" + matchupCount + ").");
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final List<Coincidence> coincidences = matchup.getCoincidences();
                for (Coincidence coincidence : coincidences) {
                    writeObservation(file, coincidence.getObservation(), matchupIndex);
                }
                writeObservation(file, referenceObservation, matchupIndex);
                writeMatchupId(file, matchupId, matchupIndex);
                persistenceManager.detach(coincidences);
            }
        } finally {
            persistenceManager.commit();
            close();
        }
    }

    private int getMatchupCount() {
        final Query query = persistenceManager.createQuery(COUNT_MATCHUPS_QUERY);
        return ((Number) query.getSingleResult()).intValue();
    }

    private void writeObservation(NetcdfFileWriteable file, Observation observation, int matchupIndex) throws
                                                                                                       Exception {
        ObservationReader io = getReader(observation);
        final Variable[] variables = io.getVariables();
        for (Variable variable : variables) {
            io.write(observation, variable, file, matchupIndex, getDimensionSizes(variable.getName()));
        }
    }

    private ObservationReader getReader(Observation observation) throws Exception {
        final String name = observation.getDatafile().getDataSchema().getName();
        ObservationReader reader = readers.get(name);
        if (reader != null) {
            return reader;
        }
        reader = ReaderFactory.createReader(name);
        reader.init(observation.getDatafile());
        readers.put(name, reader);
        return reader;
    }

    private void writeMatchupId(NetcdfFileWriteable file, int matchupId, int matchupIndex) throws IOException,
                                                                                                  InvalidRangeException {
        final Array array = Array.factory(DataType.INT, new int[]{1}, new int[]{matchupId});
        file.write("mId", new int[]{matchupIndex}, array);
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

    /**
     * Returns the lengths of the dimensions of the variable given by <code>variableName</code>.
     *
     * @param variableName The variable name to get the dimension sizes for.
     *
     * @return An array of integers containing the dimension length <code>l[i]</code> for dimension with index
     *         <code>i</code>.
     */
    private int[] getDimensionSizes(String variableName) {
        final String dimString = varDimensionMap.get(NetcdfFile.escapeName(variableName));
        final String[] dims = dimString.split(" ");
        int[] result = new int[dims.length];
        for (int i = 0; i < dims.length; i++) {
            result[i] = dimensionCountMap.get(dims[i]);
        }
        return result;
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

    @SuppressWarnings({"unchecked"})
    private void addVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
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
        varDimensionMap.put(v.getNameEscaped(), dims);
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
