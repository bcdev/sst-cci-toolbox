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
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.VariableDescriptor;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * Utility class for commonly used reader utility functions.
 *
 * @author Thomas Storm
 */
public class ReaderUtil {

    private ReaderUtil() {
    }

    public static void findTimeInterval(NetcdfFile sourceFile, Date refTime, int limit, int[] origin,
                                        int[] shape) throws IOException {
        final Variable timeVar = sourceFile.findVariable(NetcdfFile.escapeName(Constants.VARNAME_HISTORY_TIME));
        final Array times = timeVar.read();
        final double refJd = TimeUtil.julianDate(refTime);
        int startIndex = -1;
        int endIndex = (int) times.getSize();
        for (int i = 0; i < times.getSize(); i++) {
            final double time = times.getDouble(i);
            if (startIndex == -1 && time >= refJd - 0.5) {
                startIndex = i;
            }
            if (startIndex != -1 && time <= refJd + 0.5) {
                endIndex = i;
            }
        }
        if (startIndex == -1) {
            throw new NoSuchElementException(
                    MessageFormat.format("Unable to find time interval for reference time {0}", refTime.toString()));
        }

        final int size = endIndex - startIndex;
        if (size <= limit) {
            origin[0] = startIndex;
            shape[0] = size;
        } else {
            origin[0] = startIndex + (size - limit) / 2;
            shape[0] = limit;
        }
    }

    public static boolean fits(final Date refTime, final double julianDate) throws ParseException {
        final Date observationDate = TimeUtil.dateOfJulianDate(julianDate);
        final long time = observationDate.getTime();
        final int twelveHours = 12 * 60 * 60 * 1000;
        return refTime.getTime() < time + twelveHours &&
               refTime.getTime() > time - twelveHours;
    }

    public static PGgeometry createGeometry(double startLon, double startLat, double endLon, double endLat) {
        startLon = normalizeLon(startLon);
        endLon = normalizeLon(endLon);

        return new PGgeometry(new LineString(new Point[]{new Point(startLon, startLat), new Point(endLon, endLat)}));
    }

    public static Date centerTime(Date startTime, Date endTime) {
        return new Date((startTime.getTime() + endTime.getTime()) / 2);
    }

    public static long timeRadius(Date startTime, Date endTime) {
        return Math.abs(endTime.getTime() - startTime.getTime()) / 2000;
    }

    public static double normalizeLon(double lon) {
        if (lon > 180.0) {
            lon = lon - 360.0;
        }
        return lon;
    }

    public static void setVariableUnits(final Variable variable, final VariableDescriptor variableDescriptor) {
        final String units = variable.getUnitsString();
        if (units != null && !units.isEmpty()) {
            variableDescriptor.setUnits(units);
        }
    }

    public static void setVariableDescriptorAttributes(final Variable variable,
                                                       final VariableDescriptor variableDescriptor) {
        for (final Attribute attr : variable.getAttributes()) {
            if ("add_offset".equals(attr.getName())) {
                variableDescriptor.setAddOffset(attr.getNumericValue());
            }
            if ("scale_factor".equals(attr.getName())) {
                variableDescriptor.setScaleFactor(attr.getNumericValue());
            }
            if ("_FillValue".equals(attr.getName())) {
                variableDescriptor.setFillValue(attr.getNumericValue());
            }
            if ("valid_min".equals(attr.getName())) {
                variableDescriptor.setValidMin(attr.getNumericValue());
            }
            if ("valid_max".equals(attr.getName())) {
                variableDescriptor.setValidMax(attr.getNumericValue());
            }
            if ("long_name".equals(attr.getName())) {
                variableDescriptor.setLongName(attr.getStringValue());
            }
            if ("standard_name".equals(attr.getName())) {
                variableDescriptor.setStandardName(attr.getStringValue());
            }
        }
    }

    public static String setVariableDescriptorDimensions(final Variable variable,
                                                         final VariableDescriptor variableDescriptor) {
        final String dimensions = variable.getDimensionsString();
        variableDescriptor.setDimensions(dimensions);
        return dimensions;
    }

    public static VariableDescriptor createBasicVariableDescriptor(final Variable variable, final String sensorName) {
        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.setName(String.format("%s.%s", sensorName, variable.getName()));
        variableDescriptor.setType(variable.getDataType().name());
        return variableDescriptor;
    }

    public static void setVariableDesciptorDataSchema(final DataFile dataFile,
                                                      final VariableDescriptor variableDescriptor) {
        variableDescriptor.setDataSchema(dataFile.getDataSchema());
    }
}
