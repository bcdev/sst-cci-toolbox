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

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.data.Timeable;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.util.TimeUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

/**
 * TODO fill out or delete
 *
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
                                                            observation.getName(),
                                                            recordNo,
                                                            e.getMessage());
                tool.getErrorHandler().warn(e, message);
            }
        }
        return hasPersisted;
    }

    void persistColumns(final String sensorName, final IOHandler ioHandler) throws IOException {
        final Item[] columns = ioHandler.getColumns();
        tool.getLogger().info(MessageFormat.format("Number of columns for sensor ''{0}'' = {1}.",
                                              sensorName, columns.length));
        for (final Item column : columns) {
            tool.getPersistenceManager().persist(column);
        }
    }

    Sensor createSensor(String sensorName, String observationType, long pattern) {
        final SensorBuilder builder = new SensorBuilder();
        builder.setName(sensorName);
        builder.setObservationType(observationType);
        builder.setPattern(pattern).build();
        final Sensor sensor = builder.build();
        tool.getPersistenceManager().persist(sensor);
        return sensor;
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
            return TimeUtil.checkTimeOverlap(time, tool.getSourceStartTime(), tool.getSourceStopTime(), timeRadius);
        }
        // for MMD' ingestion no time is required if located=no.
        // This is represented by an observation of type Observation, not of RelatedObservation.
        // So, do not throw an exception here.
        //throw new ToolException("Expected observation with time stamp.", ToolException.TOOL_CONFIGURATION_ERROR);
        return true;
    }

}
