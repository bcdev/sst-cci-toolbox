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

import org.esa.cci.sst.data.ColumnBuilder;
import org.postgis.LineString;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.util.Date;

/**
 * Utility class for commonly used IO utility functions.
 *
 * @author Thomas Storm
 */
public class IoUtil {

    private IoUtil() {
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


    public static ColumnBuilder createColumnBuilder(final Variable variable, final String sensorName) {
        final ColumnBuilder builder = new ColumnBuilder();
        builder.setName(sensorName + "." + variable.getName());
        builder.setType(variable.getDataType());
        builder.setDimensions(variable.getDimensionsString());
        setUnits(variable, builder);
        setAttributes(variable, builder);
        builder.setRole(variable.getName());

        return builder;
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

    private static void setAttributes(final Variable variable, final ColumnBuilder builder) {
        for (final Attribute attribute : variable.getAttributes()) {
            if ("add_offset".equals(attribute.getName())) {
                builder.setAddOffset(attribute.getNumericValue());
            }
            if ("scale_factor".equals(attribute.getName())) {
                builder.setScaleFactor(attribute.getNumericValue());
            }
            if ("_FillValue".equals(attribute.getName())) {
                builder.setFillValue(attribute.getNumericValue());
            }
            if ("valid_min".equals(attribute.getName())) {
                builder.setValidMin(attribute.getNumericValue());
            }
            if ("valid_max".equals(attribute.getName())) {
                builder.setValidMax(attribute.getNumericValue());
            }
            if ("long_name".equals(attribute.getName())) {
                builder.setLongName(attribute.getStringValue());
            }
            if ("standard_name".equals(attribute.getName())) {
                builder.setStandardName(attribute.getStringValue());
            }
        }
    }

    private static void setUnits(final Variable variable, final ColumnBuilder builder) {
        final String units = variable.getUnitsString();
        if (units != null && !units.isEmpty()) {
            builder.setUnit(units);
        }
    }
}
