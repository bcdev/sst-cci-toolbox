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

import org.esa.cci.sst.data.VariableDescriptor;
import org.postgis.LineString;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.text.MessageFormat;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * Utility class for commonly used IO utility functions.
 *
 * @author Thomas Storm
 */
public class IoUtil {

    private IoUtil() {
    }

    public static void findTimeInterval(Array jds, double referenceJd, int limit, int[] origin, int[] shape) {
        int startIndex = -1;
        int endIndex = (int) jds.getSize();
        for (int i = 0; i < jds.getSize(); i++) {
            final double time = jds.getDouble(i);
            if (startIndex == -1 && time >= referenceJd - 0.5) {
                startIndex = i;
            }
            if (startIndex != -1 && time <= referenceJd + 0.5) {
                endIndex = i;
            }
        }
        if (startIndex == -1) {
            throw new NoSuchElementException(
                    MessageFormat.format("Unable to find time interval for reference time {0}",
                                         TimeUtil.toDate(referenceJd).toString()));
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

    public static Date centerTime(Date startTime, Date endTime) {
        return new Date((startTime.getTime() + endTime.getTime()) / 2);
    }

    public static PGgeometry createLineGeometry(double startLon, double startLat, double endLon, double endLat) {
        startLon = normalizeLon(startLon);
        endLon = normalizeLon(endLon);

        return new PGgeometry(new LineString(new Point[]{new Point(startLon, startLat), new Point(endLon, endLat)}));
    }

    public static PGgeometry createPolygonGeometry(double startLon, double startLat, double endLon, double endLat) {
        startLon = normalizeLon(startLon);
        endLon = normalizeLon(endLon);

        return new PGgeometry(new Polygon(new LinearRing[]{
                new LinearRing(new Point[]{
                        new Point(startLon, startLat),
                        new Point(endLon, endLat)
                })
        }));
    }

    public static VariableDescriptor createVariableDescriptor(final Variable variable, final String sensorName) {
        final VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.setName(String.format("%s.%s", sensorName, variable.getName()));
        descriptor.setType(variable.getDataType().name());
        setDimensions(variable, descriptor);
        setAttributes(variable, descriptor);
        setUnits(variable, descriptor);

        return descriptor;
    }

    public static long timeRadius(Date startTime, Date endTime) {
        return Math.abs(endTime.getTime() - startTime.getTime()) / 2000;
    }

    // this is a copy of {@code GeoPos.normalizeLon()} using {@code double} instead of {@code float} as argument
    private static double normalizeLon(double lon) {
        if (lon < -360f || lon > 360f) {
            lon %= 360f;
        }
        if (lon < -180f) {
            lon += 360f;
        } else if (lon > 180.0f) {
            lon -= 360f;
        }
        return lon;
    }

    private static void setAttributes(final Variable variable, final VariableDescriptor descriptor) {
        for (final Attribute attribute : variable.getAttributes()) {
            if ("add_offset".equals(attribute.getName())) {
                descriptor.setAddOffset(attribute.getNumericValue());
            }
            if ("scale_factor".equals(attribute.getName())) {
                descriptor.setScaleFactor(attribute.getNumericValue());
            }
            if ("_FillValue".equals(attribute.getName())) {
                descriptor.setFillValue(attribute.getNumericValue());
            }
            if ("valid_min".equals(attribute.getName())) {
                descriptor.setValidMin(attribute.getNumericValue());
            }
            if ("valid_max".equals(attribute.getName())) {
                descriptor.setValidMax(attribute.getNumericValue());
            }
            if ("long_name".equals(attribute.getName())) {
                descriptor.setLongName(attribute.getStringValue());
            }
            if ("standard_name".equals(attribute.getName())) {
                descriptor.setStandardName(attribute.getStringValue());
            }
        }
    }

    private static void setDimensions(final Variable variable, final VariableDescriptor descriptor) {
        final String dimensions = variable.getDimensionsString();
        descriptor.setDimensions(dimensions);
    }

    private static void setUnits(final Variable variable, final VariableDescriptor descriptor) {
        final String units = variable.getUnitsString();
        if (units != null && !units.isEmpty()) {
            descriptor.setUnits(units);
        }
    }
}
