/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.reader.ObservationIOHandler;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.esa.cci.sst.SensorName.*;

/**
 * Default implementation of <code>MmdGenerator</code>, writing all variables. This class provides some (package
 * private) API for creating an mmd file.
 *
 * @author Thomas Storm
 */
public class DefaultMmdGenerator implements MmdGeneratorTool.MmdGenerator {

    private final Map<String, Integer> dimensionCountMap = new HashMap<String, Integer>(17);
    private final Map<String, String> variablesDimensionsMap = new HashMap<String, String>(61);
    private final Map<String, ObservationIOHandler> readers = new HashMap<String, ObservationIOHandler>();
    private final PersistenceManager persistenceManager;

    private int matchupCount = -1;

    DefaultMmdGenerator(Properties properties) throws IOException {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, properties);
        setUpDimensionCountMap();
    }

    @Override
    public void createMmdStructure(NetcdfFileWriteable file) throws Exception {
        addDimensions(file);
        addStandardVariables(file);

        for (SensorName sensorName : SensorName.values()) {
            if (!SENSOR_NAME_AATSR_MD.getSensor().equalsIgnoreCase(sensorName.getSensor()) &&
                !SENSOR_NAME_AAI.getSensor().equalsIgnoreCase(sensorName.getSensor()) &&
                !SENSOR_NAME_INSITU.getSensor().equalsIgnoreCase(sensorName.getSensor())) {
                addObservationTime(file, sensorName.getSensor());
                addLsMask(file, sensorName.getSensor());
                addNwpData(file, sensorName.getSensor());
                addVariables(file, sensorName.getSensor());
            }
        }

        file.setLargeFile(true);
        addGlobalAttributes(file);
        file.create();
    }

    @Override
    public void writeMatchups(NetcdfFileWriteable file) throws IOException {
        // open database
        persistenceManager.transaction();
        try {
            final List<Matchup> resultList = getMatchups();
            int matchupCount = resultList.size();

            for (int matchupIndex = 0; matchupIndex < matchupCount; matchupIndex++) {
                Matchup matchup = resultList.get(matchupIndex);
                final int matchupId = matchup.getId();
                // todo - replace with logging
                System.out.println("Writing matchup '" + matchupId + "' (" + matchupIndex + "/" + matchupCount + ").");
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final List<Coincidence> coincidences = matchup.getCoincidences();
                final PGgeometry point = referenceObservation.getPoint();
                for (Coincidence coincidence : coincidences) {
                    writeObservation(file, coincidence.getObservation(), point, matchupIndex,
                                     referenceObservation.getTime());
                }
                writeObservation(file, referenceObservation, point, matchupIndex, referenceObservation.getTime());
                writeMatchupId(file, matchupId, matchupIndex);
                persistenceManager.detach(coincidences);
            }
        } finally {
            // close database
            persistenceManager.commit();
        }
    }

    @Override
    public void close() {
        for (ObservationIOHandler ioHandler : readers.values()) {
            ioHandler.close();
        }
    }

    @SuppressWarnings({"unchecked"})
    List<Matchup> getMatchups() {
        Query getAllMatchupsQuery = persistenceManager.createQuery(ALL_MATCHUPS_QUERY);
        return getAllMatchupsQuery.getResultList();
    }

    void addDimensions(final NetcdfFileWriteable file) {
        for (String dimensionName : dimensionCountMap.keySet()) {
            file.addDimension(dimensionName, dimensionCountMap.get(dimensionName));
        }
    }

    void writeObservation(NetcdfFileWriteable file, Observation observation, final PGgeometry point,
                          int matchupIndex, final Date refTime) throws IOException {
        ObservationIOHandler ioHandler = getReader(observation);
        final Variable[] variables = ioHandler.getVariables();
        for (Variable variable : variables) {
            ioHandler.write(observation, variable, file, matchupIndex, getDimensionSizes(variable.getName()), point,
                            refTime);
        }
    }

    ObservationIOHandler getReader(final Observation observation) throws IOException {
        final DataFile datafile = observation.getDatafile();
        final String path = datafile.getPath();
        if (readers.get(path) != null) {
            return readers.get(path);
        }
        ObservationIOHandler ioHandler = IOHandlerFactory.createReader(datafile.getDataSchema().getName());
        readers.put(path, ioHandler);
        ioHandler.init(datafile);
        return ioHandler;
    }

    int getMatchupCount() {
        if (matchupCount != -1) {
            return matchupCount;
        }
        final Query query = persistenceManager.createQuery(COUNT_MATCHUPS_QUERY);
        matchupCount = ((Number) query.getSingleResult()).intValue();
        return matchupCount;
    }

    void writeMatchupId(NetcdfFileWriteable file, int matchupId, int matchupIndex) throws IOException {
        final Array array = Array.factory(DataType.INT, new int[]{1}, new int[]{matchupId});
        try {
            file.write("mId", new int[]{matchupIndex}, array);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    void addGlobalAttributes(NetcdfFileWriteable file) {
        file.addGlobalAttribute("title", "SST CCI multi-sensor match-up dataset (MMD) template");
        file.addGlobalAttribute("institution", "Brockmann Consult");
        file.addGlobalAttribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)");
        file.addGlobalAttribute("creation_date", Calendar.getInstance().getTime().toString());
        file.addGlobalAttribute("total_number_of_matchups", 0);
    }

    void addInsituDataHistories(NetcdfFileWriteable file) {
        // todo: add in-situ data histories (rq-20110223)
    }

    void addNwpData(NetcdfFileWriteable file, String sensorName) {
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
    int[] getDimensionSizes(String variableName) {
        final String dimString = variablesDimensionsMap.get(NetcdfFile.escapeName(variableName));
        final String[] dims = dimString.split(" ");
        int[] result = new int[dims.length];
        for (int i = 0; i < dims.length; i++) {
            result[i] = dimensionCountMap.get(dims[i]);
        }
        return result;
    }

    void addObservationTime(NetcdfFileWriteable file, String sensorName) {
        final ucar.nc2.Variable time = file.addVariable(file.getRootGroup(),
                                                        String.format("%s.observation_time", sensorName),
                                                        DataType.DOUBLE,
                                                        String.format("match_up %s.ni", sensorName));
        addAttribute(time, "units", "Julian Date");
    }

    void addLsMask(NetcdfFileWriteable file, String sensorName) {
        file.addVariable(file.getRootGroup(),
                         String.format("%s.land_sea_mask", sensorName),
                         DataType.BYTE,
                         String.format("match_up %s.ni %s.nj", sensorName, sensorName));
    }

    @SuppressWarnings({"unchecked"})
    void addVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        final List<Variable> resultList = query.getResultList();
        for (final Variable var : resultList) {
            addVariable(file, var, createDimensionString(var, sensorName));
        }
    }

    String createDimensionString(Variable var, String sensorName) {
        final String[] dimensionNames = var.getDimensions().split(" ");
        final String[] dimensionRoles = var.getDimensionRoles().split(" ");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dimensionRoles.length; i++) {
            final String dimensionName = dimensionNames[i];
            final String dimensionRole = dimensionRoles[i];
            if (i != 0) {
                sb.append(" ");
            }
            if (!Constants.DIMENSION_ROLE_MATCHUP.equals(dimensionRole)) {
                sb.append(sensorName);
                sb.append(".");
            }
            if (!Constants.DIMENSION_ROLE_LENGTH.equals(dimensionRole)) {
                sb.append(dimensionRole);
            } else {
                sb.append(dimensionName);
            }
        }
        if (!sb.toString().contains(Constants.DIMENSION_NAME_MATCHUP)) {
            sb.insert(0, Constants.DIMENSION_NAME_MATCHUP + " ");
        }
        return sb.toString();
    }

    void addVariable(NetcdfFileWriteable file, String name, DataType type, String dims) {
        final Variable var = new Variable();
        var.setName(name);
        var.setType(type.name());
        addVariable(file, var, dims);
    }

    void addVariable(NetcdfFileWriteable file, Variable var, String dims) {
        final ucar.nc2.Variable v = file.addVariable(file.getRootGroup(),
                                                     var.getName(),
                                                     DataType.valueOf(var.getType()),
                                                     dims);
        addAttribute(v, "standard_name", var.getStandardName());
        addAttribute(v, "units", var.getUnits());
        addAttribute(v, "add_offset", var.getAddOffset(), DataType.FLOAT);
        addAttribute(v, "scale_factor", var.getScaleFactor(), DataType.FLOAT);
        addAttribute(v, "_FillValue", var.getFillValue(), v.getDataType());
        variablesDimensionsMap.put(v.getNameEscaped(), dims);
    }

    Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(
                String.format("select v from Variable v where v.name like '%s.%%' order by v.name", sensorName));
    }

    PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    private void addStandardVariables(final NetcdfFileWriteable file) {
        // todo: tie point dimensions for all sensors (rq-20110223)
        file.addVariable("mId", DataType.INT, Constants.DIMENSION_NAME_MATCHUP);
        addVariables(file, SENSOR_NAME_AATSR_MD.getSensor());
        addVariable(file, SENSOR_NAME_AAI.getSensor() + ".aai", DataType.SHORT,
                    Constants.DIMENSION_NAME_MATCHUP + " aai.ni");
        addInsituDataHistories(file);
    }

    private void setUpDimensionCountMap() {
        dimensionCountMap.put(Constants.DIMENSION_NAME_MATCHUP, getMatchupCount());
        dimensionCountMap.put("aatsr-md.cs_length", Constants.AATSR_MD_CS_LENGTH);
        dimensionCountMap.put("aatsr-md.ui_length", Constants.AATSR_MD_UI_LENGTH);
        dimensionCountMap.put("aatsr-md.length", Constants.AATSR_MD_LENGTH);
        dimensionCountMap.put("metop.ni", Constants.METOP_LENGTH);
        dimensionCountMap.put("metop.nj", Constants.METOP_LENGTH);
        dimensionCountMap.put("metop.len_id", Constants.METOP_LEN_ID);
        dimensionCountMap.put("metop.len_filename", Constants.METOP_LEN_FILENAME);
        dimensionCountMap.put("seviri.ni", Constants.SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.nj", Constants.SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.len_id", Constants.SEVIRI_LEN_ID);
        dimensionCountMap.put("seviri.len_filename", Constants.SEVIRI_LEN_FILENAME);
        dimensionCountMap.put("aatsr.ni", Constants.AATSR_LENGTH);
        dimensionCountMap.put("aatsr.nj", Constants.AATSR_LENGTH);
        dimensionCountMap.put("avhrr.ni", Constants.AVHRR_WIDTH);
        dimensionCountMap.put("avhrr.nj", Constants.AVHRR_HEIGHT);
        dimensionCountMap.put("amsre.ni", Constants.AMSRE_LENGTH);
        dimensionCountMap.put("amsre.nj", Constants.AMSRE_LENGTH);
        dimensionCountMap.put("tmi.ni", Constants.TMI_LENGTH);
        dimensionCountMap.put("tmi.nj", Constants.TMI_LENGTH);
        dimensionCountMap.put("aai.ni", Constants.AAI_LENGTH);
        dimensionCountMap.put("seaice.ni", Constants.SEA_ICE_LENGTH);
        dimensionCountMap.put("seaice.nj", Constants.SEA_ICE_LENGTH);
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
}
