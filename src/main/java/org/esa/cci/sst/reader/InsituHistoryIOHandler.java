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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DriftingObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import org.postgis.PGgeometry;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
public class InsituHistoryIOHandler extends NetcdfObservationStructureReader {

    public InsituHistoryIOHandler() {
        super(SensorName.SENSOR_NAME_INSITU.getSensor());
    }

    @Override
    public int getNumRecords() {
        return 1;
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException {
        final DriftingObservation observation = new DriftingObservation();
        final DataFile dataFile = getDataFileEntry();
        observation.setDatafile(dataFile);
        observation.setName(getNcFile().findGlobalAttribute("title").getStringValue());
        observation.setRecordNo(recordNo);
        observation.setSensor(getSensorName());
        try {
            final String path = dataFile.getPath();
            final TimeInterval time = getTime(path);
            observation.setTime(time.centralTime);
            observation.setTimeRadius(time.timeRadius);
        } catch (ParseException e) {
            throw new IOException("Unable to set time", e);
        }
        return observation;
    }

    TimeInterval getTime(final String fileName) throws ParseException {
        final String[] splittedString = fileName.split("_");
        String startTimeString = splittedString[splittedString.length - 2];
        String endTimeString = splittedString[splittedString.length - 1].split("\\.")[0];
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date startTime = dateFormat.parse(startTimeString);
        final Date endTime = dateFormat.parse(endTimeString);
        final Date centralTime = new Date((startTime.getTime() + endTime.getTime()) / 2);
        return new TimeInterval(centralTime, endTime.getTime() - centralTime.getTime());
    }

    @Override
    public void write(Observation observation, Variable variable, NetcdfFileWriteable file, int matchupIndex,
                      int[] dimensionSizes, final PGgeometry point) throws IOException {
        // todo - implement
        // todo - consider reference observation time; add to parameters
        // todo - consider gary's answer: do we need to write fitting to aatsr-buoy-id?

    }

    static class TimeInterval {

        final Date centralTime;
        final long timeRadius;

        public TimeInterval(final Date centralTime, final long timeRadius) {
            this.centralTime = centralTime;
            this.timeRadius = timeRadius;
        }
    }

}
