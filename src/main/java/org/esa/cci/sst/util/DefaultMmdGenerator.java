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
import org.esa.cci.sst.MmdGeneratorTool;
import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.esa.cci.sst.SensorType.*;

/**
 * Default implementation of <code>MmdGenerator</code>, writing all variables. This class provides some (package
 * private) API for creating an mmd file.
 *
 * @author Thomas Storm
 */
public class DefaultMmdGenerator implements MmdGeneratorTool.MmdGenerator {

    private final Map<String, Integer> dimensionCountMap = new HashMap<String, Integer>(17);
    private final Map<String, String> variablesDimensionsMap = new HashMap<String, String>(61);
    private final Map<String, IOHandler> readers = new HashMap<String, IOHandler>();
    private final PersistenceManager persistenceManager;
    private final Properties configuration;
    private final Properties targetVariables;

    private int matchupCount = 10;

    public DefaultMmdGenerator(Properties configuration, Properties targetVariables) throws IOException {
        this.configuration = configuration;
        this.targetVariables = targetVariables;
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, configuration);
        initDimensionCountMap();
    }

    @Override
    public void createMmdStructure(NetcdfFileWriteable file) throws Exception {
        addDimensions(file);
        file.addVariable("matchup_id", DataType.INT, Constants.DIMENSION_NAME_MATCHUP);

        for (final Map.Entry<Object, Object> entry : configuration.entrySet()) {
            if (entry.getKey().toString().matches("mms.test.inputSets.[0-9]+.sensor")) {
                final String sensorName = entry.getValue().toString();
                final SensorType sensorType = SensorType.getSensorType(sensorName);
                addVariables(file, sensorType, sensorName);
                if (sensorType == ATSR || sensorType == AVHRR || sensorType == AMSRE || sensorType == TMI) {
                    addObservationTime(file, sensorType, sensorName);
                    addLsMask(file, sensorType, sensorName);
                    addNwpData(file, sensorType, sensorName);
                }
            }
        }

        addGlobalAttributes(file);
        file.setLargeFile(true);
        file.create();
    }

    @Override
    public void writeMatchups(NetcdfFileWriteable file) throws IOException {
        persistenceManager.transaction();
        try {
            final List<Matchup> resultList = getMatchups();
            //final int matchupCount = resultList.size();

            for (int matchupIndex = 0; matchupIndex < matchupCount; matchupIndex++) {
                Matchup matchup = resultList.get(matchupIndex);
                final int matchupId = matchup.getId();
                // todo - replace with logging
                System.out.println("Writing matchup '" + matchupId + "' (" + matchupIndex + "/" + matchupCount + ").");
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final List<Coincidence> coincidences = matchup.getCoincidences();
                final PGgeometry point = referenceObservation.getPoint();
                writeMatchupId(file, matchupId, matchupIndex);
                writeObservation(file, referenceObservation, point, matchupIndex, referenceObservation.getTime());
                for (final Coincidence coincidence : coincidences) {
                    final Observation observation = coincidence.getObservation();
                    if (!AVHRR.isSensor(observation.getSensor())) {
                        writeObservation(file, observation, point, matchupIndex, referenceObservation.getTime());
                    }
                }
                persistenceManager.detach(coincidences);
            }
        } finally {
            persistenceManager.commit();
        }
    }

    @Override
    public void close() {
        for (IOHandler ioHandler : readers.values()) {
            ioHandler.close();
        }
    }

    @SuppressWarnings({"unchecked"})
    List<Matchup> getMatchups() {
        //return persistenceManager.createQuery(ALL_MATCHUPS_QUERY).getResultList();
        return persistenceManager.createNativeQuery("select m.id from mm_matchup m where m.pattern & 31 = 31 order by m.id asc;", Matchup.class).getResultList();
    }

    void addDimensions(final NetcdfFileWriteable file) {
        // todo - sort according to dimension name
        for (String dimensionName : dimensionCountMap.keySet()) {
            file.addDimension(dimensionName, dimensionCountMap.get(dimensionName));
        }
    }

    void writeObservation(NetcdfFileWriteable file, Observation observation, final PGgeometry point,
                          int matchupIndex, final Date refTime) throws IOException {
        final IOHandler ioHandler = getIOHandler(observation);
        for (final VariableDescriptor descriptor : ioHandler.getVariableDescriptors()) {
            if (targetVariables.isEmpty() || targetVariables.containsKey(descriptor.getName())) {
                final String sourceVariableName = descriptor.getBasename();
                final String targetVariableName = getTargetVariableName(descriptor.getName());
                ioHandler.write(file, observation, sourceVariableName, targetVariableName, matchupIndex, point,
                                refTime);
            }
        }
    }

    private String getTargetVariableName(String name) {
        final String result = targetVariables.getProperty(name);
        if (result == null || result.isEmpty()) {
            return name;
        }
        return result;
    }

    IOHandler getIOHandler(final Observation observation) throws IOException {
        final DataFile datafile = observation.getDatafile();
        final String path = datafile.getPath();
        if (readers.get(path) != null) {
            return readers.get(path);
        }
        IOHandler ioHandler = IOHandlerFactory.createHandler(datafile.getDataSchema().getName(),
                                                             observation.getSensor());
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
            file.write("matchup_id", new int[]{matchupIndex}, array);
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

    void addNwpData(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        // todo: add NWP data (rq-20110223)
    }

    void addObservationTime(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final String variableName = String.format("%s.observation_time", sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable time = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.DOUBLE,
                                                   String.format("match_up %s.ni", sensorType));
            addAttribute(time, "units", "Julian Date");
        }
    }

    void addLsMask(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final String variableName = String.format("%s.land_sea_mask", sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable mask = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.BYTE,
                                                   String.format("match_up %s.ni %s.nj", sensorType, sensorType));
            addAttribute(mask, "_FillValue", Byte.MIN_VALUE, DataType.BYTE);
        }
    }

    void addVariables(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final List<VariableDescriptor> descriptorList = query.getResultList();
        for (final VariableDescriptor descriptor : descriptorList) {
            if (targetVariables.isEmpty() || targetVariables.containsKey(descriptor.getName())) {
                addVariable(file, descriptor, createDimensionString(descriptor, sensorType));
            }
        }
    }

    String createDimensionString(VariableDescriptor var, SensorType sensorType) {
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
                sb.append(sensorType);
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

    void addVariable(NetcdfFileWriteable file, VariableDescriptor descriptor, String dims) {
        final DataType dataType = DataType.valueOf(descriptor.getType());
        final String targetVariableName = getTargetVariableName(descriptor.getName());
        final Variable v = file.addVariable(file.getRootGroup(), targetVariableName, dataType, dims);
        addAttribute(v, "standard_name", descriptor.getStandardName());
        addAttribute(v, "units", descriptor.getUnits());
        addAttribute(v, "add_offset", descriptor.getAddOffset(), DataType.FLOAT);
        addAttribute(v, "scale_factor", descriptor.getScaleFactor(), DataType.FLOAT);
        addAttribute(v, "_FillValue", descriptor.getFillValue(), v.getDataType());
        variablesDimensionsMap.put(v.getName(), dims);
    }

    Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(String.format(
                "select v from VariableDescriptor v where v.name like '%s.%%' order by v.name", sensorName));
    }

    private void initDimensionCountMap() {
        dimensionCountMap.put(Constants.DIMENSION_NAME_MATCHUP, getMatchupCount());
        // todo: use properties instead of constants (rq-20110329)
        dimensionCountMap.put("atsr_md.cs_length", Constants.ATSR_MD_CS_LENGTH);
        dimensionCountMap.put("atsr_md.ui_length", Constants.ATSR_MD_UI_LENGTH);
        dimensionCountMap.put("atsr_md.length", Constants.ATSR_MD_LENGTH);
        dimensionCountMap.put("metop.ni", Constants.METOP_LENGTH);
        dimensionCountMap.put("metop.nj", Constants.METOP_LENGTH);
        dimensionCountMap.put("metop.len_id", Constants.METOP_LEN_ID);
        dimensionCountMap.put("metop.len_filename", Constants.METOP_LEN_FILENAME);
        dimensionCountMap.put("seviri.ni", Constants.SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.nj", Constants.SEVIRI_LENGTH);
        dimensionCountMap.put("seviri.len_id", Constants.SEVIRI_LEN_ID);
        dimensionCountMap.put("seviri.len_filename", Constants.SEVIRI_LEN_FILENAME);
        dimensionCountMap.put("atsr.ni", Constants.ATSR_SUBSCENE_HEIGHT);
        dimensionCountMap.put("atsr.nj", Constants.ATSR_SUBSCENE_WIDTH);
        dimensionCountMap.put("avhrr.ni", Constants.AVHRR_SUBSCENE_WIDTH);
        dimensionCountMap.put("avhrr.nj", Constants.AVHRR_SUBSCENE_HEIGHT);
        dimensionCountMap.put("amsre.ni", Constants.AMSRE_SUBSCENE_HEIGHT);
        dimensionCountMap.put("amsre.nj", Constants.AMSRE_SUBSCENE_WIDTH);
        dimensionCountMap.put("tmi.ni", Constants.TMI_SUBSCENE_HEIGHT);
        dimensionCountMap.put("tmi.nj", Constants.TMI_SUBSCENE_WIDTH);
        dimensionCountMap.put("aai.ni", Constants.AAI_SUBSCENE_HEIGHT);
        dimensionCountMap.put("aai.nj", Constants.AAI_SUBSCENE_WIDTH);
        dimensionCountMap.put("seaice.ni", Constants.SEA_ICE_SUBSCENE_HEIGHT);
        dimensionCountMap.put("seaice.nj", Constants.SEA_ICE_SUBSCENE_WIDTH);
        dimensionCountMap.put("history.time", Constants.INSITU_HISTORY_LENGTH);
        dimensionCountMap.put("history.qc_length", Constants.INSITU_HISTORY_QC_LENGTH);
        // todo: NWP tie point dimensions for all sensors (rq-20110223)
    }

    private static void addAttribute(Variable v, String attrName, String attrValue) {
        if (attrValue != null) {
            v.addAttribute(new Attribute(attrName, attrValue));
        }
    }

    private static void addAttribute(Variable v, String attrName, Number attrValue, DataType attrType) {
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
                    throw new IllegalArgumentException(MessageFormat.format(
                            "Attribute type ''{0}'' is not supported", attrType.toString()));
            }
        }
    }
}
