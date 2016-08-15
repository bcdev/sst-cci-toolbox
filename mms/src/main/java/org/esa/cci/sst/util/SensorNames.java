package org.esa.cci.sst.util;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

public class SensorNames {

    private SensorNames() {
    }


    public static String getOrbitName(String sensorName) {
        if (isOrbitName(sensorName)) {
            return sensorName;
        }
        if (isStandardName(sensorName)) {
            return "orb_" + sensorName;
        }
        throw new IllegalArgumentException("Sensor name '" + sensorName + "' does not match expected name patterns.");
    }

    public static String getStandardName(String sensorName) {
        if (isStandardName(sensorName)) {
            return sensorName;
        }
        if (isOrbitName(sensorName)) {
            return sensorName.substring(4);
        }
        throw new IllegalArgumentException("Sensor name '" + sensorName + "' does not match expected name patterns.");
    }

    public static String getBasename(String sensorName) {
        final String standardName = getStandardName(sensorName);
        final int dotIndex = standardName.indexOf(".");

        final String basename;
        if (dotIndex == -1) {
            basename = standardName;
        } else {
            basename = standardName.substring(0, dotIndex);
        }

        return basename;
    }

    public static String getDimensionNameX(String sensorName) {
        return getBasename(sensorName) + ".nx";
    }

    public static String getDimensionNameY(String sensorName) {
        return getBasename(sensorName) + ".ny";
    }

    public static boolean isOrbitName(String sensorName) {
        return sensorName.matches("(orb_atsr\\.[1-3])|(orb_avhrr(_f)?\\.[mn]([0-9]){2})|(orb_amsr(2|e))");
    }

    public static boolean isStandardName(String sensorName) {
        return sensorName.matches("(atsr\\.[1-3])|(avhrr(_f)?\\.[mn]([0-9]){2})|(amsr(2|e))");
    }
}
