/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.ColumnI;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.reader.InsituVariable;
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Allows to create the structure for the output mmd file.
 *
 * @author Thomas Storm
 */
class MmdStructureGenerator {

    private final MmdGeneratorTool tool;
    private final MmdGenerator generator;
    private final Map<String, Integer> dimensionCountMap = new TreeMap<String, Integer>();
    private final Properties targetVariables;

    MmdStructureGenerator(final MmdGeneratorTool tool, final MmdGenerator generator) {
        this.tool = tool;
        this.generator = generator;
        targetVariables = generator.getTargetVariables();
        initDimensionCountMap();
    }

    void createMmdStructure(NetcdfFileWriteable file) throws Exception {
        addDimensions(file);
        addMatchupVariables(file);
        addWatermaskVariables(file);
        addInsituReferenceVariables(file);
        addAllInputVariables(file);
        addGlobalAttributes(file);
    }

    void addDimensions(final NetcdfFileWriteable file) {
        for (final Map.Entry<String, Integer> stringIntegerEntry : dimensionCountMap.entrySet()) {
            file.addDimension(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
        }
    }

    private void addAllInputVariables(final NetcdfFileWriteable file) {
        String sensorName;
        final Properties configuration = tool.getConfiguration();
        int i = 0;
        while ((sensorName = getSensor(configuration, i)) != null) {
            addInputVariables(file, sensorName);
            // todo - mb ts, 29Apr2011 - replace by configuration
            final boolean addVariables = false;
//            final boolean addVariables = sensorName == ATSR || sensorName == AVHRR || sensorName == AMSRE || sensorName == TMI;
            if (addVariables) {
                addObservationTimeVariable(file, sensorName);
                addLsMaskVariable(file, sensorName);
                addNwpData(file, sensorName);
            }
            i++;
        }
    }

    private String getSensor(final Properties configuration, final int i) {
        return configuration.getProperty(String.format("mms.source.%d.sensor", i));
    }

    private void addMatchupVariables(final NetcdfFileWriteable file) {
        file.addVariable(Constants.VARIABLE_NAME_MATCHUP_ID, DataType.INT, Constants.DIMENSION_NAME_MATCHUP);
        file.addVariable(Constants.VARIABLE_NAME_TIME, DataType.DOUBLE, Constants.DIMENSION_NAME_MATCHUP);
        file.addVariable(Constants.VARIABLE_NAME_LON, DataType.FLOAT, Constants.DIMENSION_NAME_MATCHUP);
        file.addVariable(Constants.VARIABLE_NAME_LAT, DataType.FLOAT, Constants.DIMENSION_NAME_MATCHUP);
    }

    private void addNwpData(NetcdfFileWriteable file, String sensorName) {
        // todo: add NWP data (rq-20110223)
    }

    private void addWatermaskVariables(final NetcdfFileWriteable file) {
        final String propertyVarNameValue = tool.getConfiguration().getProperty("mmd.watermask.target.variablename");
        final String propertyXDimensionValue = tool.getConfiguration().getProperty("mmd.watermask.target.xdimension");
        final String propertyYDimensionValue = tool.getConfiguration().getProperty("mmd.watermask.target.ydimension");
        String[] variableNames = propertyVarNameValue.split(" ");
        String[] xDimensionNames = propertyXDimensionValue.split(" ");
        String[] yDimensionNames = propertyYDimensionValue.split(" ");
        for (int i = 0; i < variableNames.length; i++) {
            final String variableName = variableNames[i];
            final String xDimension = xDimensionNames[i];
            final String yDimension = yDimensionNames[i];
            final String dimensions = String.format("%s %s %s", Constants.DIMENSION_NAME_MATCHUP, xDimension,
                                                    yDimension);
            final Variable watermaskVariable = file.addVariable(variableName, DataType.SHORT, dimensions);
            watermaskVariable.addAttribute(new Attribute("land_value", 0));
            watermaskVariable.addAttribute(new Attribute("water_value", 1));
            watermaskVariable.addAttribute(new Attribute("invalid_value", 2));
        }
    }

    private void addLsMaskVariable(NetcdfFileWriteable file, String sensorName) {
        final String variableName = String.format("%s.land_sea_mask", sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable mask = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.BYTE,
                                                   String.format("%s %s.ni %s.nj", Constants.DIMENSION_NAME_MATCHUP,
                                                                 sensorName, sensorName));
            addAttribute(mask, "_FillValue", Byte.MIN_VALUE, DataType.BYTE);
        }
    }

    private void addObservationTimeVariable(NetcdfFileWriteable file, String sensorName) {
        final String variableName = String.format("%s." + Constants.VARIABLE_OBSERVATION_TIME, sensorName);
        if (targetVariables.isEmpty() || targetVariables.containsKey(variableName)) {
            final Variable time = file.addVariable(file.getRootGroup(),
                                                   getTargetVariableName(variableName),
                                                   DataType.DOUBLE,
                                                   String.format("%s %s.ni", Constants.DIMENSION_NAME_MATCHUP,
                                                                 sensorName));
            addAttribute(time, "units", "Julian Date");
        }
    }

    private void addInsituReferenceVariables(NetcdfFileWriteable file) {
        for (final InsituVariable v : InsituVariable.values()) {
            final String prefixedName = "reference." + v.getName();
            if (targetVariables.isEmpty() || targetVariables.containsKey(prefixedName)) {
                final Variable variable = file.addVariable(file.getRootGroup(),
                                                           getTargetVariableName(prefixedName),
                                                           v.getDataType(),
                                                           Constants.DIMENSION_NAME_MATCHUP);
                for (final Attribute a : v.getAttributes()) {
                    variable.addAttribute(a);
                }
            }
        }
    }

    private void addInputVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = tool.getPersistenceManager().createQuery(String.format(
                "select v from Column v where v.name like '%s.%%' order by v.name", sensorName));
        @SuppressWarnings({"unchecked"})
        final List<ColumnI> columnList = new ArrayList<ColumnI>(query.getResultList());
        for (final ColumnI column : columnList) {
            if (targetVariables.isEmpty() || targetVariables.containsKey(column.getName())) {
                addVariable(file, column);
            }
        }
    }

    private void addGlobalAttributes(NetcdfFileWriteable file) {
        file.addGlobalAttribute("title", "SST CCI multi-sensor match-up dataset (MMD) template");
        file.addGlobalAttribute("institution", "Brockmann Consult");
        file.addGlobalAttribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)");
        file.addGlobalAttribute("creation_date", Calendar.getInstance().getTime().toString());
        file.addGlobalAttribute("total_number_of_matchups", getMatchups().size());
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

    private void addVariable(NetcdfFileWriteable targetFile, ColumnI column) {
        // todo - apply column rules here (rq-20110420)
        final DataType dataType = DataType.valueOf(column.getType());
        final String targetVariableName = getTargetVariableName(column.getName());
        final String dimensions = column.getDimensions();
        if (targetFile.findVariable(NetcdfFile.escapeName(targetVariableName)) == null) {
            final Variable v = targetFile.addVariable(targetFile.getRootGroup(), targetVariableName, dataType,
                                                      dimensions);
            addAttribute(v, "standard_name", column.getStandardName());
            addAttribute(v, "units", column.getUnit());
            addAttribute(v, "add_offset", column.getAddOffset(), DataType.FLOAT);
            addAttribute(v, "scale_factor", column.getScaleFactor(), DataType.FLOAT);
            addAttribute(v, "_FillValue", column.getFillValue(), v.getDataType());
        }
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

    private String getTargetVariableName(String name) {
        return generator.getTargetVariableName(name);
    }

    private List<Matchup> getMatchups() {
        return generator.getMatchups();
    }

}
