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

import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DriftingObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
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
public class InsituHistoryIOHandler extends NetcdfStructureIOHandler {

    private static final String VARNAME_DRIFTER_TIME = "drifter.time";

    public InsituHistoryIOHandler() {
        super(SensorType.INSITU.nameLowerCase());
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
    public void write(NetcdfFileWriteable file, Observation observation, Variable variable,
                      int matchupIndex, int[] dimensionSizes, PGgeometry refPoint, Date refTime) throws IOException {
        final NetcdfFile sourceFile = getNcFile();
        final ucar.nc2.Variable sourceVariable = sourceFile.findVariable(NetcdfFile.escapeName(variable.getName()));
        int[] origin = createOrigin(matchupIndex, dimensionSizes.length);
        int[] shape = createShape(matchupIndex, dimensionSizes);
        try {
            Array array;
            final ucar.nc2.Variable timeVar = sourceFile.findVariable(NetcdfFile.escapeName(VARNAME_DRIFTER_TIME));
            final Array drifterTimeValue = timeVar.read(createOrigin(matchupIndex, 1), new int[]{1});
            if (fits(refTime, drifterTimeValue.getDouble(0))) {
                array = sourceVariable.read(origin, shape);
            } else {
                array = createFillArray(sourceVariable.getDataType(), null, shape);
            }
            file.write(NetcdfFile.escapeName(variable.getName()), array);
        } catch (Exception e) {
            throw new IOException(e);
        }

        // todo - consider gary's answer: do we need to write fitting to aatsr-buoy-id?
    }

    Array createFillArray(final DataType dataType, Object fillValue, final int[] shape) {
        int size = 1;
        for (int length : shape) {
            size *= length;
        }
        final Array array = Array.factory(dataType, shape);
        for(int i = 0; i < size; i++) {
            array.setObject(i, fillValue);
        }
        return array;
    }

    boolean fits(final Date refTime, final double julianDate) throws ParseException {
        final Date observationDate = TimeUtil.dateOfJulianDate(julianDate);
        final long time = observationDate.getTime();
        final int twelveHours = 12 * + 60 * 60 * 1000;
        return refTime.getTime() < time + twelveHours &&
               refTime.getTime() > time - twelveHours;
    }

    private int[] createOrigin(final int matchupIndex, final int length) {
        final int[] origin = new int[length];
        origin[0] = matchupIndex;
        for (int i = 1; i < length; i++) {
            origin[i] = 0;
        }
        return origin;
    }

    private int[] createShape(final int length, final int dimensionSizes[]) {
        final int[] shape = new int[length];
        shape[0] = 1;
        System.arraycopy(dimensionSizes, 1, shape, 1, length - 1);
        return shape;
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
