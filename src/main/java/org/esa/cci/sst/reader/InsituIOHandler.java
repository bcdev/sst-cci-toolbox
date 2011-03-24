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
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
public class InsituIOHandler extends NetcdfStructureIOHandler {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private static final String VARNAME_HISTORY_TIME = "history.insitu.time";

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public InsituIOHandler() {
        super(SensorType.HISTORY.getSensor());
    }

    @Override
    public int getNumRecords() {
        return 1;
    }

    @Override
    public InsituObservation readObservation(int recordNo) throws IOException {
        final InsituObservation observation = new InsituObservation();
        final DataFile dataFile = getDataFile();
        observation.setDatafile(dataFile);
        observation.setName(getNcFile().findGlobalAttribute("aoml_id").getStringValue());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());
        try {
            final Date startTime = parseDate("start_date");
            final Date endTime = parseDate("drog_off_date");
            observation.setTime(centerTime(startTime, endTime));
            observation.setTimeRadius(timeRadius(startTime, endTime));
        } catch (ParseException e) {
            throw new IOException("Unable to set time.", e);
        }
        try {
            final double startLon = parseDouble("start_lon");
            final double startLat = parseDouble("start_lat");
            final double endLon = parseDouble("end_lon");
            final double endLat = parseDouble("end_lat");
            observation.setLocation(geometry(startLon, startLat, endLon, endLat));
        } catch (ParseException e) {
            throw new IOException("Unable to set location.", e);
        }
        return observation;
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
            final ucar.nc2.Variable timeVar = sourceFile.findVariable(NetcdfFile.escapeName(VARNAME_HISTORY_TIME));
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

    static Array createFillArray(final DataType dataType, Object fillValue, final int[] shape) {
        int size = 1;
        for (int length : shape) {
            size *= length;
        }
        final Array array = Array.factory(dataType, shape);
        for (int i = 0; i < size; i++) {
            array.setObject(i, fillValue);
        }
        return array;
    }

    static boolean fits(final Date refTime, final double julianDate) throws ParseException {
        final Date observationDate = TimeUtil.dateOfJulianDate(julianDate);
        final long time = observationDate.getTime();
        final int twelveHours = 12 * 60 * 60 * 1000;
        return refTime.getTime() < time + twelveHours &&
               refTime.getTime() > time - twelveHours;
    }

    private static int[] createOrigin(final int matchupIndex, final int length) {
        final int[] origin = new int[length];
        origin[0] = matchupIndex;
        for (int i = 1; i < length; i++) {
            origin[i] = 0;
        }
        return origin;
    }

    private static int[] createShape(final int length, final int dimensionSizes[]) {
        final int[] shape = new int[length];
        shape[0] = 1;
        System.arraycopy(dimensionSizes, 1, shape, 1, length - 1);
        return shape;
    }

    private static PGgeometry geometry(double startLon, double startLat, double endLon, double endLat) {
        return new PGgeometry(new LineString(new Point[]{new Point(startLon, startLat), new Point(endLon, endLat)}));
    }

    private Date parseDate(String attributeName) throws ParseException {
        return DATE_FORMAT.parse(getNcFile().findGlobalAttribute(attributeName).getStringValue());
    }

    private double parseDouble(String attributeName) throws ParseException {
        return Double.parseDouble(getNcFile().findGlobalAttribute(attributeName).getStringValue());
    }

    private static Date centerTime(Date startTime, Date endTime) {
        return new Date((startTime.getTime() + endTime.getTime()) / 2);
    }

    private static long timeRadius(Date startTime, Date endTime) {
        return Math.abs(endTime.getTime() - startTime.getTime()) / 2;
    }
}
