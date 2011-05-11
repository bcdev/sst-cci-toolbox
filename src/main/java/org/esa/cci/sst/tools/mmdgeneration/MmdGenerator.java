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

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.InsituRecord;
import org.esa.cci.sst.reader.InsituVariable;
import org.esa.cci.sst.reader.MmdIOHandler;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import javax.naming.OperationNotSupportedException;
import javax.persistence.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Default implementation of <code>MmdGenerator</code>, writing all variables. This class provides some (package
 * private) API for creating an mmd file.
 *
 * @author Thomas Storm
 */
@Deprecated
class MmdGenerator {

    static final String TIME_CONSTRAINED_MATCHUPS_QUERY =
            "select m.id"
            + " from mm_matchup m, mm_observation o"
            + " where o.id = m.refobs_id"
            + " and o.time > TIMESTAMP WITH TIME ZONE '%s'"
            + " and o.time < TIMESTAMP WITH TIME ZONE '%s'"
            + " order by o.time";

    private final PersistenceManager persistenceManager;
    private final Properties targetVariables;
    private final BasicTool tool;
    private final List<Matchup> matchupList = new ArrayList<Matchup>();
    private Map<String, IOHandler> ioHandlerMap = new HashMap<String, IOHandler>();

    @Deprecated
    MmdGenerator(final BasicTool tool) throws IOException {
        this.tool = tool;
        final String propertiesFilePath = tool.getConfiguration().getProperty("mmd.output.variables");
        final InputStream is = new FileInputStream(propertiesFilePath);

        this.targetVariables = new Properties();
        targetVariables.load(is);
        persistenceManager = tool.getPersistenceManager();
    }

    void writeMatchups(NetcdfFileWriteable file) throws IOException {
        persistenceManager.transaction();
        try {
            final List<Matchup> resultList = getMatchups();
            final int matchupCount = resultList.size();
            final LandWaterMaskWriter landWaterMaskWriter = new LandWaterMaskWriter(file, tool);
            for (int matchupIndex = 0; matchupIndex < matchupCount; matchupIndex++) {
                final Matchup matchup = resultList.get(matchupIndex);
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final int matchupId = matchup.getId();
                tool.getLogger().info(
                        MessageFormat.format("Writing matchup ''{0}'' ({1}/{2}).", matchupId, matchupIndex + 1,
                                             matchupCount));
                final List<Coincidence> coincidences = matchup.getCoincidences();
                final PGgeometry point = referenceObservation.getPoint();
                // todo - optimize: search ref. point only once per subs-scene (rq-20110403)
                writeMatchupId(file, matchupId, matchupIndex);
                writeObservation(file, referenceObservation, point, matchupIndex, referenceObservation.getTime());
                writeInsitu(file, matchupIndex, referenceObservation, "reference.");
                writeInsitu(file, matchupIndex, referenceObservation, "history.");
                writeMatchupTime(file, matchupIndex, referenceObservation);
                writeLocation(file, matchupIndex, referenceObservation);
                for (final Coincidence coincidence : coincidences) {
                    final Observation observation = coincidence.getObservation();
                    writeObservation(file, observation, point, matchupIndex, referenceObservation.getTime());
                }
                landWaterMaskWriter.writeLandWaterMask(matchupIndex);
                persistenceManager.detach(coincidences);
            }
        } finally {
            persistenceManager.commit();
        }
    }

    void writeObservation(NetcdfFileWriteable file, Observation observation, final PGgeometry point,
                          int matchupIndex, final Date refTime) throws IOException {
        IOHandler ioHandler = null;
        try {
            ioHandler = createIOHandler(observation);
            for (final Item column : ioHandler.getColumns()) {
                if (targetVariables.isEmpty() || targetVariables.containsKey(column.getName())) {
                    final String sourceVariableName = column.getRole();
                    final String targetVariableName = getTargetVariableName(column.getName());
                    try {
                        ioHandler.write(file, observation, sourceVariableName, targetVariableName, matchupIndex, point,
                                        refTime);
                    } catch (Exception e) {
                        tool.getErrorHandler().warn(e, MessageFormat.format(
                                "Unable to write data for observation ''{0}'': {1}", observation, e.getMessage()));
                    }
                } else {
                    tool.getLogger().fine(MessageFormat.format("Skipping variable ''{0}''.", column.getName()));
                }
            }
        } finally {
            // todo - optimize: keep some files open or loop over source data files (rq-20110403)
            if (ioHandler != null) {
                ioHandler.close();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    List<Matchup> getMatchups() {
        if (matchupList.isEmpty()) {
            final String startTime = tool.getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_START_TIME);
            final String endTime = tool.getConfiguration().getProperty(Constants.PROPERTY_OUTPUT_END_TIME);
            final String queryString = String.format(TIME_CONSTRAINED_MATCHUPS_QUERY, startTime, endTime);
            final Query query = persistenceManager.createNativeQuery(queryString, Matchup.class);
            matchupList.addAll(query.getResultList());
        }
        return Collections.unmodifiableList(matchupList);

//        final List<Matchup> matchups = new ArrayList<Matchup>();
//        for (int i = 0; i < 10; i++) {
//            matchups.add(matchupList.get(i));
//        }
//        return matchups;
    }

    String getTargetVariableName(String name) {
        final String result = targetVariables.getProperty(name);
        if (result == null || result.isEmpty()) {
            return name;
        }
        return result;
    }

    Properties getTargetVariables() {
        final Properties properties = new Properties();
        properties.putAll(targetVariables);
        return properties;
    }

    private void writeMatchupId(NetcdfFileWriteable file, int matchupId, int matchupIndex) throws IOException {
        final Array array = Array.factory(DataType.INT, new int[]{1}, new int[]{matchupId});
        try {
            file.write(NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID), new int[]{matchupIndex}, array);
        } catch (InvalidRangeException e) {
            throw new ToolException("Unable to write matchup id.", e, ToolException.TOOL_ERROR);
        }
    }

    private void writeMatchupTime(final NetcdfFileWriteable file, final int matchupIndex,
                                  final ReferenceObservation referenceObservation) {
        final Date referenceObservationTime = referenceObservation.getTime();
        final Array time = Array.factory(DataType.LONG, new int[]{1}, new long[]{referenceObservationTime.getTime()});
        try {
            file.write(NetcdfFile.escapeName(Constants.VARIABLE_NAME_TIME), new int[]{matchupIndex}, time);
        } catch (Exception e) {
            throw new ToolException("Unable to write time.", e, ToolException.TOOL_ERROR);
        }
    }

    private void writeLocation(final NetcdfFileWriteable file, final int matchupIndex,
                               final ReferenceObservation referenceObservation) {
        final Point point = referenceObservation.getPoint().getGeometry().getFirstPoint();
        float lon = (float) point.getX();
        float lat = (float) point.getY();
        final Array lonArray = Array.factory(DataType.FLOAT, new int[]{1}, new float[]{lon});
        final Array latArray = Array.factory(DataType.FLOAT, new int[]{1}, new float[]{lat});
        try {
            file.write(NetcdfFile.escapeName(Constants.VARIABLE_NAME_LON), new int[]{matchupIndex}, lonArray);
            file.write(NetcdfFile.escapeName(Constants.VARIABLE_NAME_LAT), new int[]{matchupIndex}, latArray);
        } catch (Exception e) {
            throw new ToolException("Unable to write location.", e, ToolException.TOOL_ERROR);
        }
    }

    private void writeInsitu(NetcdfFileWriteable targetFile, int targetRecordNo,
                             ReferenceObservation referenceObservation, String prefix) throws IOException {
        IOHandler handler = null;
        try {
            handler = createIOHandler(referenceObservation);
            final InsituRecord record;
            try {
                record = handler.readInsituRecord(referenceObservation.getRecordNo());
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e); // cannot happen
            }
            for (final InsituVariable v : InsituVariable.values()) {
                final String prefixedName = prefix + v.getName();
                if (targetVariables.isEmpty() || targetVariables.containsKey(prefixedName)) {
                    final Number variableValue = record.getValue(v);
                    targetFile.write(NetcdfFile.escapeName(prefixedName), new int[]{targetRecordNo, 0},
                                     toArray2D(variableValue));
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    private static Array toArray2D(Number value) {
        final Array array = Array.factory(value.getClass(), new int[]{1, 1});
        array.setObject(0, value);
        return array;
    }

    private IOHandler createIOHandler(Observation observation) throws IOException {
        final String sensorName = observation.getSensor();
        IOHandler handler = ioHandlerMap.get(sensorName);
        if (handler == null) {
            handler = new MmdIOHandler(tool.getConfiguration());
            ioHandlerMap.put(sensorName, handler);
        }
        final DataFile handlerDataFile = handler.getDataFile();
        if (observation.getDatafile() != handlerDataFile) {
            if (handlerDataFile != null) {
                handler.close();
            }
            handler.init(observation.getDatafile());
        }
        return handler;
    }
}
