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

package org.esa.cci.sst;

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
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.esa.cci.sst.SensorType.*;

/**
 * Default implementation of <code>MmdGenerator</code>, writing all variables. This class provides some (package
 * private) API for creating an mmd file.
 *
 * @author Thomas Storm
 */
class DefaultMmdGenerator implements MmdGenerator {

    private final Map<String, Integer> dimensionCountMap = new TreeMap<String, Integer>();
    private final Map<Observation, IOHandler> ioHandlerMap = new HashMap<Observation, IOHandler>();
    private final PersistenceManager persistenceManager;
    private final Properties targetVariables;
    private final MmsTool tool;

    DefaultMmdGenerator(final MmsTool tool) throws IOException {
        this.tool = tool;
        final String propertiesFilePath = tool.getConfiguration().getProperty("mmd.output.variables");
        final InputStream is = new FileInputStream(propertiesFilePath);
        this.targetVariables = new Properties();
        targetVariables.load(is);
        persistenceManager = tool.getPersistenceManager();
        initDimensionCountMap();
    }

    @Override
    public void createMmdStructure(NetcdfFileWriteable file) throws Exception {
        addDimensions(file);
        file.addVariable("matchup_id", DataType.INT, Constants.DIMENSION_NAME_MATCHUP);

        for (int i = 0; i < 100; i++) {
            final String sensorName =
                    tool.getConfiguration().getProperty(String.format("mms.test.inputSets.%d.sensor", i));
            if (sensorName != null) {
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
            final int matchupCount = resultList.size();

            for (int matchupIndex = 0; matchupIndex < matchupCount; matchupIndex++) {
                final Matchup matchup = resultList.get(matchupIndex);
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final int matchupId = matchup.getId();
                tool.getLogger().info(
                        MessageFormat.format("Writing matchup ''{0}'' ({1}/{2}).", matchupId, matchupIndex,
                                             matchupCount));
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
        for (final IOHandler ioHandler : ioHandlerMap.values()) {
            ioHandler.close();
        }
        ioHandlerMap.clear();
    }

    @SuppressWarnings({"unchecked"})
    List<Matchup> getMatchups() {
        String startTime = tool.getConfiguration().getProperty("mms.test.startTime");
        String endTime = tool.getConfiguration().getProperty("mms.test.endTime");

        String queryString = String.format(TIME_CONSTRAINED_MATCHUPS_QUERY, startTime, endTime);
        final Query query = persistenceManager.createNativeQuery(queryString, Matchup.class);
//        query.setParameter(1, "'" + startTime + "'");
//        query.setParameter(2, "'" + endTime + "'");
        return query.getResultList();
    }

    void addDimensions(final NetcdfFileWriteable file) {
        for (final String dimensionName : dimensionCountMap.keySet()) {
            file.addDimension(dimensionName, dimensionCountMap.get(dimensionName));
        }
    }

    void writeObservation(NetcdfFileWriteable file, Observation observation, final PGgeometry point,
                          int matchupIndex, final Date refTime) throws IOException {
        final IOHandler ioHandler = getIOHandler(observation);
        try {
            for (final VariableDescriptor descriptor : ioHandler.getVariableDescriptors()) {
                if (targetVariables.isEmpty() || targetVariables.containsKey(descriptor.getName())) {
                    final String sourceVariableName = descriptor.getBasename();
                    final String targetVariableName = getTargetVariableName(descriptor.getName());
                    try {
                        ioHandler.write(file, observation, sourceVariableName, targetVariableName, matchupIndex, point,
                                        refTime);
                    } catch (Exception e) {
                        tool.getErrorHandler().handleWarning(e, MessageFormat.format(
                                "Unable to write data for observation ''{0}'': {1}", observation, e.getMessage()));
                    }
                }
            }
        } finally {
            close();
        }
    }

    private String getTargetVariableName(String name) {
        final String result = targetVariables.getProperty(name);
        if (result == null || result.isEmpty()) {
            return name;
        }
        return result;
    }

    private IOHandler getIOHandler(final Observation observation) throws IOException {
        if (ioHandlerMap.containsKey(observation)) {
            return ioHandlerMap.get(observation);
        }
        final DataFile dataFile = observation.getDatafile();
        final IOHandler ioHandler = IOHandlerFactory.createHandler(dataFile.getDataSchema().getName(),
                                                                   observation.getSensor());
        ioHandler.init(dataFile);
        ioHandlerMap.put(observation, ioHandler);
        return ioHandler;
    }

    private void writeMatchupId(NetcdfFileWriteable file, int matchupId, int matchupIndex) throws IOException {
        final Array array = Array.factory(DataType.INT, new int[]{1}, new int[]{matchupId});
        try {
            file.write("matchup_id", new int[]{matchupIndex}, array);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    private void addGlobalAttributes(NetcdfFileWriteable file) {
        file.addGlobalAttribute("title", "SST CCI multi-sensor match-up dataset (MMD) template");
        file.addGlobalAttribute("institution", "Brockmann Consult");
        file.addGlobalAttribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)");
        file.addGlobalAttribute("creation_date", Calendar.getInstance().getTime().toString());
        file.addGlobalAttribute("total_number_of_matchups", 0);
    }

    private void addNwpData(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        // todo: add NWP data (rq-20110223)
    }

    private void addObservationTime(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final String variableName = String.format("%s.observation_time", sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable time = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.DOUBLE,
                                                   String.format("match_up %s.ni", sensorType));
            addAttribute(time, "units", "Julian Date");
        }
    }

    private void addLsMask(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final String variableName = String.format("%s.land_sea_mask", sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable mask = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.BYTE,
                                                   String.format("match_up %s.ni %s.nj", sensorType, sensorType));
            addAttribute(mask, "_FillValue", Byte.MIN_VALUE, DataType.BYTE);
        }
    }

    private void addVariables(NetcdfFileWriteable file, SensorType sensorType, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final ArrayList<VariableDescriptor> descriptorList = new ArrayList<VariableDescriptor>(query.getResultList());
        Collections.sort(descriptorList, new Comparator<VariableDescriptor>() {
            @Override
            public int compare(VariableDescriptor o1, VariableDescriptor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (final VariableDescriptor descriptor : descriptorList) {
            if (targetVariables.isEmpty() || targetVariables.containsKey(descriptor.getName())) {
                addVariable(file, descriptor, createDimensionString(descriptor, sensorType));
            }
        }
    }

    private String createDimensionString(VariableDescriptor var, SensorType sensorType) {
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

    private void addVariable(NetcdfFileWriteable targetFile, VariableDescriptor descriptor, String dims) {
        final DataType dataType = DataType.valueOf(descriptor.getType());
        final String targetVariableName = getTargetVariableName(descriptor.getName());
        if (targetFile.findVariable(NetcdfFile.escapeName(targetVariableName)) == null) {
            final Variable v = targetFile.addVariable(targetFile.getRootGroup(), targetVariableName, dataType, dims);
            addAttribute(v, "standard_name", descriptor.getStandardName());
            addAttribute(v, "units", descriptor.getUnits());
            addAttribute(v, "add_offset", descriptor.getAddOffset(), DataType.FLOAT);
            addAttribute(v, "scale_factor", descriptor.getScaleFactor(), DataType.FLOAT);
            addAttribute(v, "_FillValue", descriptor.getFillValue(), v.getDataType());
        }
    }

    private Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(String.format(
                "select v from VariableDescriptor v where v.name like '%s.%%' order by v.name", sensorName));
    }

    private void initDimensionCountMap() {
        dimensionCountMap.put(Constants.DIMENSION_NAME_MATCHUP, getMatchups().size());
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
