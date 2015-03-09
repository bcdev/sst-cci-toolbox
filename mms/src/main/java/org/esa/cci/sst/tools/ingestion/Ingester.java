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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.*;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.TimeUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Thomas Storm
 */
class Ingester {

    private final BasicTool tool;

    Ingester(BasicTool tool) {
        this.tool = tool;
    }

    boolean persistObservation(final Observation observation, final int recordNo) throws IOException {
        boolean hasPersisted = false;
        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        if (checkTime(observation)) {
            try {
                persistenceManager.persist(observation);
                hasPersisted = true;
            } catch (IllegalArgumentException e) {
                final String message = MessageFormat.format("Observation {0} {1} is incomplete: {2}",
                        observation.getId(),
                        recordNo,
                        e.getMessage());
                tool.getErrorHandler().warn(e, message);
            }
        }
        return hasPersisted;
    }

    void persistColumns(final String sensorName, final Reader reader) throws IOException {
        final Item[] columns = reader.getColumns();
        final Logger logger = SstLogging.getLogger();
        logger.info(MessageFormat.format("Number of columns for sensor ''{0}'' = {1}.", sensorName, columns.length));
        final ColumnStorage columnStorage = tool.getPersistenceManager().getColumnStorage();
        final List<String> existingVariables = columnStorage.getAllColumnNames();
        for (final Item column : columns) {
            if (!existingVariables.contains(column.getName())) {
                columnStorage.store((Column) column);
            }
        }
    }

    // @todo 3 tb/tb make static and write tests tb 2015-02-26
    Sensor createSensor(String sensorName, String observationType, long pattern) {
        final SensorBuilder builder = new SensorBuilder();
        builder.name(sensorName);
        builder.observationType(observationType);
        builder.pattern(pattern);

        return builder.build();
    }

    private boolean checkTime(Observation observation) {
        if (observation instanceof Timeable) {
            final Date time = ((Timeable) observation).getTime();
            final double timeRadius;
            if (observation instanceof InsituObservation) {
                timeRadius = ((InsituObservation) observation).getTimeRadius();
            } else {
                timeRadius = 0.0;
            }
            final Configuration config = tool.getConfig();
            if (config.containsValue(Configuration.KEY_MMS_INGESTION_START_TIME)) {
                final TimeRange timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_INGESTION_START_TIME,
                        Configuration.KEY_MMS_INGESTION_STOP_TIME,
                        config);
                return TimeUtil.checkTimeOverlap(time, timeRange.getStartDate(), timeRange.getStopDate(), timeRadius);
            } else {
                return true;
            }
        }
        // for MMD' ingestion no time is required if located=no.
        // This is represented by an observation of type Observation, not of RelatedObservation.
        // So, do not throw an exception here.
        //throw new ToolException("Expected observation with time stamp.", ToolException.TOOL_CONFIGURATION_ERROR);
        return true;
    }
}
